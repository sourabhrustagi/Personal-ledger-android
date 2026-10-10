package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.ThresholdWatch
import com.ledger.personal.data.local.MockTickerSocket
import com.ledger.personal.data.local.TickerSocket
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State and actions for the fx alerts screen. */
@HiltViewModel
class FxAlertsViewModel @Inject constructor(private val socket: TickerSocket) : ViewModel() {
    constructor() : this(MockTickerSocket())

    private var watch = ThresholdWatch(DEFAULT_THRESHOLD)
    private val state = MutableStateFlow(FxUiState())
    val uiState: StateFlow<FxUiState> = state.asStateFlow()

    /** Start watching. */
    fun startWatching() {
        viewModelScope.launch {
            socket.prices()
                .catch { state.update { it.copy(errorMessage = "Rate feed unavailable") } }
                .collect { check(it) }
        }
    }

    /** Reacts to permission. */
    fun onPermission(granted: Boolean) {
        if (granted) {
            state.update { it.copy(permissionDenied = false) }
        } else {
            state.update { it.copy(permissionDenied = true, status = "Notifications off") }
        }
    }

    /** Stores threshold. */
    fun setThreshold(minor: Long) {
        if (minor <= 0L) return
        watch = ThresholdWatch(minor)
        state.update { it.copy(thresholdMinor = minor, triggered = false, status = "Watching") }
    }

    /** Check. */
    fun check(rate: Long) {
        state.update { it.copy(rateMinor = rate, errorMessage = null) }
        if (state.value.permissionDenied) return
        val fired = watch.onRate(rate)
        state.update {
            it.copy(
                firedCount = it.firedCount + if (fired) 1 else 0,
                triggered = it.triggered || fired,
                status = if (fired) "Alert fired" else "No new crossing",
            )
        }
    }
}
