package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.local.MockTickerSocket
import com.ledger.personal.data.local.TickerSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** State and actions for the ticker screen. */
@OptIn(FlowPreview::class)
@HiltViewModel
class TickerViewModel @Inject constructor(socket: TickerSocket) : ViewModel() {
    constructor() : this(MockTickerSocket())

    private val usingPoll = MutableStateFlow(false)
    val polling: StateFlow<Boolean> = usingPoll.asStateFlow()

    val ticks: StateFlow<Long> = socket.prices()
        .catch { error ->
            if (error is CancellationException) throw error
            usingPoll.value = true
            emitAll(poll())
        }
        .sample(200)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val uiState: StateFlow<TickerUiState> = combine(ticks, polling) { tick, isPolling ->
        TickerUiState(tick = tick, isPolling = isPolling)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TickerUiState())

    /** Poll. `delay` is the cancellation check; a cancelled collector stops the loop. */
    private fun poll(): Flow<Long> = flow {
        var price = 8_300L
        while (true) {
            emit(price)
            price += 1L
            delay(1_000)
        }
    }
}
