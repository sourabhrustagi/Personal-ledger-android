package com.ledger.personal.data.repository

import com.ledger.personal.data.remote.api.TransactionNetworkDataSource
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.asExternalModel
import com.ledger.personal.data.remote.safeApiCall
import com.ledger.personal.di.IoDispatcher
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.VersionConflict
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Reads and writes transactions through remote network endpoints. */
@Singleton
class TransactionsRepository @Inject constructor(
    private val networkDataSource: TransactionNetworkDataSource,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionRemote {

    /** Returns transactions. */
    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> =
        safeApiCall(ioDispatcher) {
            networkDataSource.getTransactions(page = page, pageSize = pageSize).items.asExternalModel()
        }

    /** Returns transaction by id. */
    override suspend fun getTransactionById(id: String): Result<Transaction> =
        safeApiCall(ioDispatcher) {
            networkDataSource.getTransactionById(id = id).asExternalModel()
        }


    /** Creates transaction. */
    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction> = safeApiCall(ioDispatcher) {
        val request = CreateTransactionRequestDto(
            amountInCents = amountInCents,
            currency = currency,
            category = category,
            description = description
        )
        call { networkDataSource.createTransaction(request) }
    }

    /** Patch transaction. */
    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?,
    ): Result<Transaction> = safeApiCall(ioDispatcher) {
        val request = PatchTransactionRequestDto(
            category = category,
            description = description,
            amountInCents = amountInCents,
        )
        call { networkDataSource.patchTransaction(id = id, request = request) }
    }

    /** Deletes transaction. */
    override suspend fun deleteTransaction(id: String): Result<Unit> =
        safeApiCall(ioDispatcher) {
            val response = networkDataSource.deleteTransaction(id = id)
            require(response.success) { "Failed to delete transaction" }
            Unit
        }

    /** Call. */
    private suspend inline fun call(block: suspend () -> TransactionDto): Transaction = try {
        block().asExternalModel()
    } catch (error: retrofit2.HttpException) {
        throw conflictOrNull(error.code(), error.response()?.errorBody()?.string()) ?: error
    } catch (error: ClientRequestException) {
        throw conflictOrNull(error.response.status.value, error.response.bodyAsText()) ?: error
    }
}

private val conflictJson = Json { ignoreUnknownKeys = true }

/** A 409 whose body is a transaction becomes a [VersionConflict]. Anything else is left to the caller. */
fun conflictOrNull(code: Int, body: String?): VersionConflict? {
    if (code != 409 || body.isNullOrBlank()) return null
    val dto = runCatching {
        conflictJson.decodeFromString(TransactionDto.serializer(), body)
    }.getOrNull() ?: return null
    return runCatching { VersionConflict(dto.asExternalModel()) }.getOrNull()
}

/** Backward-compatible typealias for [TransactionsRepository]. */
@Deprecated(
    message = "Renamed to TransactionsRepository",
    replaceWith = ReplaceWith("TransactionsRepository")
)
typealias KtorTransactionRepository = TransactionsRepository
