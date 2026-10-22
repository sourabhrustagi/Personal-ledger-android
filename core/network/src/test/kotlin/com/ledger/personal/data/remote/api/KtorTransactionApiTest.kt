package com.ledger.personal.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class KtorTransactionApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun listUsesTheConfiguredHostAndPagingQuery() = runBlocking {
        var requested = ""
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requested = chain.request().url.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"items":[],"page":2,"page_size":25,"total_items":0}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()

        val retrofitApi = Retrofit.Builder()
            .baseUrl("https://api.example/retail/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TransactionRetrofitApi::class.java)

        val dataSource = RetrofitTransactionNetworkDataSource(retrofitApi, Dispatchers.Unconfined)
        val response = dataSource.getTransactions(page = 2, pageSize = 25)

        assertEquals(2, response.page)
        assertEquals("https://api.example/retail/v1/transactions?page=2&pageSize=25", requested)
    }

    @Test
    fun deleteReturnsDeleteTransactionResponseDto() = runBlocking {
        var requestedMethod = ""
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestedMethod = chain.request().method
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """{"success":true,"id":"tx-123"}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()

        val retrofitApi = Retrofit.Builder()
            .baseUrl("https://api.example/retail/v1/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(TransactionRetrofitApi::class.java)

        val dataSource = RetrofitTransactionNetworkDataSource(retrofitApi, Dispatchers.Unconfined)
        val response = dataSource.deleteTransaction("tx-123")

        assertEquals("DELETE", requestedMethod)
        assertTrue(response.success)
        assertEquals("tx-123", response.id)
    }
}
