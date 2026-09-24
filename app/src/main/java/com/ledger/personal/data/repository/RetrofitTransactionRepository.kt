package com.ledger.personal.data.repository

import com.ledger.personal.data.remote.api.TransactionApiService
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.toDomain
import com.ledger.personal.di.IoDispatcher
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RetrofitTransactionRepository @Inject constructor(
    private val apiService: TransactionApiService,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TransactionRepository {

    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> =
        withContext(ioDispatcher) {
            runCatching {
                val response = apiService.getTransactions(page = page, pageSize = pageSize)
                response.items.map { it.toDomain() }
            }
        }

    override suspend fun getTransactionById(id: String): Result<Transaction> =
        withContext(ioDispatcher) {
            runCatching {
                val dto = apiService.getTransactionById(id = id)
                dto.toDomain()
            }
        }

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
            val dto = apiService.createTransaction(request)
            dto.toDomain()
        }
    }

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
            val dto = apiService.patchTransaction(id = id, request = request)
            dto.toDomain()
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val response = apiService.deleteTransaction(id = id)
                if (!response.isSuccessful) {
                    throw IllegalStateException("Failed to delete transaction: HTTP ${response.code()}")
                }
            }
        }
}
