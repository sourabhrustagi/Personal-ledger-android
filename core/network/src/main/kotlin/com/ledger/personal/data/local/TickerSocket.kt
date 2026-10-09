package com.ledger.personal.data.local

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

interface TickerSocket {
    fun prices(): Flow<Long>
}

class FakeTickerSocket : TickerSocket {
    override fun prices(): Flow<Long> = callbackFlow {
        var ticks = 8_300L
        while (isActive) {
            trySend(ticks)
            ticks += 1L
            delay(200)
        }
        awaitClose { }
    }
}

class OkHttpTickerSocket(
    private val client: OkHttpClient,
    private val url: String,
) : TickerSocket {
    override fun prices(): Flow<Long> = callbackFlow {
        val socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    text.toLongOrNull()?.let { trySend(it) }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    close(t)
                }
            },
        )
        awaitClose { socket.close(1000, null) }
    }
}
