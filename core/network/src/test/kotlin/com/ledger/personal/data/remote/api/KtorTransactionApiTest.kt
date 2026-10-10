package com.ledger.personal.data.remote.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class KtorTransactionApiTest {
    @Test
    fun listUsesTheConfiguredHostAndPagingQuery() = runBlocking {
        var requested = ""
        val engine = MockEngine { request ->
            requested = request.url.toString()
            respond(
                """{"items":[],"page":2,"page_size":25,"total_items":0}""",
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(DefaultRequest) { url("https://api.example/retail/v1/") }
        }

        val response = KtorTransactionApi(client).getTransactions(page = 2, pageSize = 25)

        assertEquals(2, response.page)
        assertEquals("https://api.example/retail/v1/transactions?page=2&pageSize=25", requested)
        client.close()
    }
}
