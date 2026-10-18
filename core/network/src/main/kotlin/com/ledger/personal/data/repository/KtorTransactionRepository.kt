package com.ledger.personal.data.repository

import com.ledger.personal.data.remote.api.TransactionApi
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.toDomain
import com.ledger.personal.di.IoDispatcher
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.VersionConflict
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** Reads and writes transactions through Ktor. */
@Singleton
class KtorTransactionRepository @Inject constructor(
    private val apiService: TransactionApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionRemote {

    /** Returns transactions. */
    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> =
        withContext(ioDispatcher) {
            runCatching {
                val response = apiService.getTransactions(page = page, pageSize = pageSize)
                response.items.map { it.toDomain() }
            }
        }

    /** Returns transaction by id. */
    override suspend fun getTransactionById(id: String): Result<Transaction> =
        withContext(ioDispatcher) {
            runCatching {
                val dto = apiService.getTransactionById(id = id)
                dto.toDomain()
            }
        }

    /** Creates transaction. */
    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction> = withContext(ioDispatcher) {
        runCatching {
            val request = CreateTransactionRequestDto(
                amountInCents = amountInCents,
                currency = currency,
                category = category,
                description = description
            )
            call { apiService.createTransaction(request) }
        }
    }

    /** Patch transaction. */
    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?
    ): Result<Transaction> = withContext(ioDispatcher) {
        runCatching {
            val request = PatchTransactionRequestDto(
                category = category,
                description = description,
                amountInCents = amountInCents
            )
            call { apiService.patchTransaction(id = id, request = request) }
        }
    }

    /** Deletes transaction. */
    override suspend fun deleteTransaction(id: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                apiService.deleteTransaction(id = id)
            }
        }

    /** Call. */
    private suspend inline fun call(block: suspend () -> TransactionDto): Transaction = try {
        block().toDomain()
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
    return runCatching { VersionConflict(dto.toDomain()) }.getOrNull()
}
