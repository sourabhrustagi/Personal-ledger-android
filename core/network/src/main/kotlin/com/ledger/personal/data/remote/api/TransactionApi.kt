package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.DeleteTransactionResponseDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.TransactionListResponseDto
import com.ledger.personal.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source contract for transactions, following Google Android Architecture Samples
 * and Modern Android Architecture standards.
 */
interface TransactionNetworkDataSource {
    /** Returns transactions. */
    suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto

    /** Returns transaction by id. */
    suspend fun getTransactionById(id: String): TransactionDto

    /** Creates transaction. */
    suspend fun createTransaction(request: CreateTransactionRequestDto): TransactionDto

    /** Patch transaction. */
    suspend fun patchTransaction(id: String, request: PatchTransactionRequestDto): TransactionDto

    /** Deletes transaction. */
    suspend fun deleteTransaction(id: String): DeleteTransactionResponseDto
}

/**
 * Backward-compatible alias for [TransactionNetworkDataSource].
 */
typealias TransactionApi = TransactionNetworkDataSource

/**
 * Retrofit REST API interface for transactions.
 */
interface TransactionRetrofitApi {
    /** Returns transactions. */
    @GET(ApiEndpoints.TRANSACTIONS)
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int,
    ): TransactionListResponseDto

    /** Returns transaction by id. */
    @GET("${ApiEndpoints.TRANSACTIONS}/{id}")
    suspend fun getTransactionById(@Path("id") id: String): TransactionDto

    /** Creates transaction. */
    @POST(ApiEndpoints.TRANSACTIONS)
    suspend fun createTransaction(
        @Body request: CreateTransactionRequestDto,
    ): TransactionDto

    /** Patch transaction. */
    @PATCH("${ApiEndpoints.TRANSACTIONS}/{id}")
    suspend fun patchTransaction(
        @Path("id") id: String,
        @Body request: PatchTransactionRequestDto,
    ): TransactionDto

    /** Deletes transaction. */
    @DELETE("${ApiEndpoints.TRANSACTIONS}/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): DeleteTransactionResponseDto
}

/**
 * Retrofit-backed [TransactionNetworkDataSource] ensuring main-safety on [ioDispatcher].
 */
@Singleton
class RetrofitTransactionNetworkDataSource @Inject constructor(
    private val networkApi: TransactionRetrofitApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TransactionNetworkDataSource {

    override suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto =
        withContext(ioDispatcher) {
            networkApi.getTransactions(page = page, pageSize = pageSize)
        }

    override suspend fun getTransactionById(id: String): TransactionDto =
        withContext(ioDispatcher) {
            networkApi.getTransactionById(id = id)
        }

    override suspend fun createTransaction(request: CreateTransactionRequestDto): TransactionDto =
        withContext(ioDispatcher) {
            networkApi.createTransaction(request = request)
        }

    override suspend fun patchTransaction(id: String, request: PatchTransactionRequestDto): TransactionDto =
        withContext(ioDispatcher) {
            networkApi.patchTransaction(id = id, request = request)
        }

    override suspend fun deleteTransaction(id: String): DeleteTransactionResponseDto =
        withContext(ioDispatcher) {
            networkApi.deleteTransaction(id = id)
        }
}
