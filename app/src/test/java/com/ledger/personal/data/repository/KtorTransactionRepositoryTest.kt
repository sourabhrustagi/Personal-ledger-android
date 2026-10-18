package com.ledger.personal.data.repository

import com.ledger.personal.data.remote.api.TransactionApi
import com.ledger.personal.data.remote.dto.CreateTransactionRequestDto
import com.ledger.personal.data.remote.dto.PatchTransactionRequestDto
import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.data.remote.dto.TransactionListResponseDto
import com.ledger.personal.testing.sampleDto
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorTransactionRepositoryTest {

    private val dispatcher = StandardTestDispatcher()

    @Test
    fun getTransactionsMapsItems() = runTest(dispatcher) {
        val api = MockTransactionApi().apply {
            listResponse = TransactionListResponseDto(
                items = listOf(sampleDto(id = "a"), sampleDto(id = "b", amountInCents = 100L)),
                page = 1,
                pageSize = 15,
                totalItems = 2
            )
        }
        val repo = KtorTransactionRepository(api, dispatcher)
        val items = repo.getTransactions(1, 15).getOrThrow()
        assertEquals(listOf("a", "b"), items.map { it.id })
        assertEquals(100L, items[1].amountInCents)
        assertEquals(1, api.lastPage)
        assertEquals(15, api.lastPageSize)
    }

    @Test
    fun getTransactionsFailureIsResultFailure() = runTest(dispatcher) {
        val api = MockTransactionApi().apply {
            listError = IllegalStateException("network down")
        }
        val result = KtorTransactionRepository(api, dispatcher).getTransactions(1, 10)
        assertTrue(result.isFailure)
        assertEquals("network down", result.exceptionOrNull()?.message)
    }

    @Test
    fun getByIdMapsDto() = runTest(dispatcher) {
        val api = MockTransactionApi().apply { byIdResponse = sampleDto(id = "detail") }
        val tx = KtorTransactionRepository(api, dispatcher).getTransactionById("detail").getOrThrow()
        assertEquals("detail", tx.id)
        assertEquals("detail", api.lastGetId)
    }

    @Test
    fun createSendsRequestAndMapsResponse() = runTest(dispatcher) {
        val api = MockTransactionApi().apply { createResponse = sampleDto(id = "new", amountInCents = 4500L) }
        val tx = KtorTransactionRepository(api, dispatcher)
            .createTransaction(4500L, "USD", "Dining", "Dinner")
            .getOrThrow()
        assertEquals("new", tx.id)
        assertEquals(CreateTransactionRequestDto(4500L, "USD", "Dining", "Dinner"), api.lastCreate)
    }

    @Test
    fun patchSendsOnlyProvidedFields() = runTest(dispatcher) {
        val api = MockTransactionApi().apply {
            patchResponse = sampleDto(id = "p", category = "Office")
        }
        val tx = KtorTransactionRepository(api, dispatcher)
            .patchTransaction(id = "p", category = "Office")
            .getOrThrow()
        assertEquals("Office", tx.category)
        assertEquals("p", api.lastPatchId)
        assertEquals(PatchTransactionRequestDto(category = "Office"), api.lastPatch)
    }

    @Test
    fun deleteSucceedsOn2xx() = runTest(dispatcher) {
        val api = MockTransactionApi()
        val result = KtorTransactionRepository(api, dispatcher).deleteTransaction("gone")
        assertTrue(result.isSuccess)
        assertEquals("gone", api.lastDeleteId)
    }

    @Test
    fun deleteFailsOnHttpError() = runTest(dispatcher) {
        val api = MockTransactionApi().apply {
            deleteError = IllegalStateException("HTTP 404")
        }
        val result = KtorTransactionRepository(api, dispatcher).deleteTransaction("missing")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("HTTP 404"))
    }

    private class MockTransactionApi : TransactionApi {
        var listResponse = TransactionListResponseDto(emptyList(), 1, 15, 0)
        var listError: Throwable? = null
        var byIdResponse = sampleDto()
        var byIdError: Throwable? = null
        var createResponse = sampleDto()
        var patchResponse = sampleDto()
        var deleteError: Throwable? = null

        var lastPage: Int? = null
        var lastPageSize: Int? = null
        var lastGetId: String? = null
        var lastCreate: CreateTransactionRequestDto? = null
        var lastPatchId: String? = null
        var lastPatch: PatchTransactionRequestDto? = null
        var lastDeleteId: String? = null

        override suspend fun getTransactions(page: Int, pageSize: Int): TransactionListResponseDto {
            lastPage = page
            lastPageSize = pageSize
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

        override suspend fun deleteTransaction(id: String) {
            lastDeleteId = id
            deleteError?.let { throw it }
        }
    }
}
