package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.TransactionListResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface TransactionApiService {

    @GET("transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): TransactionListResponseDto

    @GET("transactions/{id}")
    suspend fun getTransactionById(
        @Path("id") id: String
    ): TransactionDto

    @POST("transactions")
    suspend fun createTransaction(
        @Body request: CreateTransactionRequestDto
    ): TransactionDto

    @PATCH("transactions/{id}")
    suspend fun patchTransaction(
        @Path("id") id: String,
        @Body request: PatchTransactionRequestDto
    ): TransactionDto

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(
        @Path("id") id: String
    ): Response<Unit>
}
