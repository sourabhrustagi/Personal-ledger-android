package com.ledger.personal.data.local

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Contract for ticker socket. */
fun interface TickerSocket {
    /** Prices. */
    fun prices(): Flow<Long>
}

/** Mock ticker socket. */
class MockTickerSocket : TickerSocket {
    /** Prices. */
    override fun prices(): Flow<Long> = flow {
        var ticks = 8_300L
        while (true) {
            emit(ticks)
            ticks += 1L
            delay(200)
        }
    }
}

/** Ktor WebSocket ticker. */
class KtorTickerSocket(
    private val client: HttpClient,
    private val url: String,
) : TickerSocket {
    /** Prices. */
    override fun prices(): Flow<Long> = flow {
        client.webSocket(urlString = url) {
            for (frame in incoming) {
                if (frame is Frame.Text) frame.readText().toLongOrNull()?.let { emit(it) }
            }
        }
    }
}
