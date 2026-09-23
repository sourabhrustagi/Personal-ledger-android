package com.ledger.personal.data.repository

import com.ledger.personal.data.remote.api.TransactionApiService
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.TransactionListResponseDto
import com.ledger.personal.testing.sampleDto
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RetrofitTransactionRepositoryTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun getTransactionsMapsItems() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply {
            listResponse = TransactionListResponseDto(
                items = listOf(sampleDto(id = "a"), sampleDto(id = "b", amountInCents = 100L)),
                page = 1,
                pageSize = 15,
                totalItems = 2
            )
        }
        val repo = RetrofitTransactionRepository(api, dispatcher)
        val items = repo.getTransactions(1, 15).getOrThrow()
        assertEquals(listOf("a", "b"), items.map { it.id })
        assertEquals(100L, items[1].amountInCents)
        assertEquals(1 to 15, api.lastListArgs)
    }

    @Test
    fun getTransactionsFailureIsResultFailure() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply {
            listError = IllegalStateException("network down")
        }
        val result = RetrofitTransactionRepository(api, dispatcher).getTransactions(1, 10)
        assertTrue(result.isFailure)
        assertEquals("network down", result.exceptionOrNull()?.message)
    }

    @Test
    fun getByIdMapsDto() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply { byIdResponse = sampleDto(id = "detail") }
        val tx = RetrofitTransactionRepository(api, dispatcher).getTransactionById("detail").getOrThrow()
        assertEquals("detail", tx.id)
        assertEquals("detail", api.lastGetId)
    }

    @Test
    fun createSendsRequestAndMapsResponse() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply { createResponse = sampleDto(id = "new", amountInCents = 4500L) }
        val tx = RetrofitTransactionRepository(api, dispatcher)
            .createTransaction(4500L, "USD", "Dining", "Dinner")
            .getOrThrow()
        assertEquals("new", tx.id)
        assertEquals(CreateTransactionRequestDto(4500L, "USD", "Dining", "Dinner"), api.lastCreate)
    }

    @Test
    fun patchSendsOnlyProvidedFields() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply {
            patchResponse = sampleDto(id = "p", category = "Office")
        }
        val tx = RetrofitTransactionRepository(api, dispatcher)
            .patchTransaction(id = "p", category = "Office")
            .getOrThrow()
        assertEquals("Office", tx.category)
        assertEquals("p", api.lastPatchId)
        assertEquals(PatchTransactionRequestDto(category = "Office"), api.lastPatch)
    }

    @Test
    fun deleteSucceedsOn2xx() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply { deleteResponse = Response.success(Unit) }
        val result = RetrofitTransactionRepository(api, dispatcher).deleteTransaction("gone")
        assertTrue(result.isSuccess)
        assertEquals("gone", api.lastDeleteId)
    }

    @Test
    fun deleteFailsOnHttpError() = runTest(dispatcher) {
        val api = FakeTransactionApiService().apply {
            deleteResponse = Response.error(404, ByteArray(0).toResponseBody(null))
        }
        val result = RetrofitTransactionRepository(api, dispatcher).deleteTransaction("missing")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("HTTP 404"))
    }

    private class FakeTransactionApiService : TransactionApiService {
        var listResponse = TransactionListResponseDto(emptyList(), 1, 15, 0)
        var listError: Throwable? = null
        var byIdResponse = sampleDto()
        var byIdError: Throwable? = null
        var createResponse = sampleDto()
        var patchResponse = sampleDto()
        var deleteResponse: Response<Unit> = Response.success(Unit)

        var lastListArgs: Pair<Int, Int>? = null
        var lastGetId: String? = null
        var lastCreate: CreateTransactionRequestDto? = null
        var lastPatchId: String? = null
        var lastPatch: PatchTransactionRequestDto? = null
        var lastDeleteId: String? = null

        override suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto {
            lastListArgs = page to pageSize
            listError?.let { throw it }
            return listResponse
        }

        override suspend fun getTransactionById(id: String): TransactionDto {
            lastGetId = id
            byIdError?.let { throw it }
            return byIdResponse
        }

        override suspend fun createTransaction(request: CreateTransactionRequestDto): TransactionDto {
            lastCreate = request
            return createResponse
        }

        override suspend fun patchTransaction(id: String, request: PatchTransactionRequestDto): TransactionDto {
            lastPatchId = id
            lastPatch = request
            return patchResponse
        }

        override suspend fun deleteTransaction(id: String): Response<Unit> {
            lastDeleteId = id
            return deleteResponse
        }
    }
}
