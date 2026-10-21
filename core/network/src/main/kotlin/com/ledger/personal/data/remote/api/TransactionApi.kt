package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.TransactionListResponseDto
import com.ledger.personal.di.AuthenticatedClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.appendPathSegments
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Transaction HTTP contract. */
interface TransactionApi {
    /** Returns transactions. */
    suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto

    /** Returns transaction by id. */
    suspend fun getTransactionById(id: String): TransactionDto

    /** Creates transaction. */
    suspend fun createTransaction(request: CreateTransactionRequestDto): TransactionDto

    /** Patch transaction. */
    suspend fun patchTransaction(id: String, request: PatchTransactionRequestDto): TransactionDto

    /** Deletes transaction. */
    suspend fun deleteTransaction(id: String)
}

/** Ktor implementation of the transaction API. */
@Singleton
class KtorTransactionApi @Inject constructor(
    @param:AuthenticatedClient private val client: HttpClient,
) : TransactionApi {
    /** Returns transactions. */
    override suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto =
        client.get(ApiEndpoints.TRANSACTIONS) {
            url {
                parameters.append("page", page.toString())
                parameters.append("pageSize", pageSize.toString())
            }
        }.body()

    /** Returns transaction by id. */
    override suspend fun getTransactionById(id: String): TransactionDto =
        client.get { url { appendPathSegments(ApiEndpoints.TRANSACTIONS, id) } }.body()

    /**
     * Creates transaction. The key is fixed for this call, so the retry plugin resends the same one.
     * ponytail: a WorkManager re-run sends a new key; pass the local row id once the server accepts it.
     */
    override suspend fun createTransaction(request: CreateTransactionRequestDto): TransactionDto =
        client.post(ApiEndpoints.TRANSACTIONS) {
            header("Idempotency-Key", UUID.randomUUID().toString())
            setBody(request)
        }.body()

    /** Patch transaction. */
    override suspend fun patchTransaction(id: String, request: PatchTransactionRequestDto): TransactionDto =
        client.patch {
            url { appendPathSegments(ApiEndpoints.TRANSACTIONS, id) }
            setBody(request)
        }.body()

    /** Deletes transaction. */
    override suspend fun deleteTransaction(id: String) {
        client.delete { url { appendPathSegments(ApiEndpoints.TRANSACTIONS, id) } }
    }
}
