package com.ledger.personal.feature.history.voyager

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Voyager Screen contract. */
interface Screen {
    val key: String get() = this::class.java.name

    @Composable
    fun Content()
}

/**
 * Voyager ScreenModel base class.
 * Retained lifecycle-aware state holder with its own [screenModelScope].
 */
abstract class ScreenModel : RememberObserver {
    val screenModelScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("VoyagerScreenModel"))

    override fun onRemembered() = Unit

    override fun onForgotten() {
        onDispose()
    }

    override fun onAbandoned() {
        onDispose()
    }

    open fun onDispose() {
        screenModelScope.cancel()
    }
}

/**
 * Voyager StateScreenModel managing a reactive [StateFlow].
 */
abstract class StateScreenModel<STATE : Any>(initialState: STATE) : ScreenModel() {
    protected val mutableState: MutableStateFlow<STATE> = MutableStateFlow(initialState)
    val state: StateFlow<STATE> = mutableState.asStateFlow()

    protected fun updateState(updater: (STATE) -> STATE) {
        mutableState.update(updater)
    }
}

/** Retains a [ScreenModel] instance across recompositions. */
@Composable
inline fun <reified T : ScreenModel> rememberScreenModel(
    tag: String? = null,
    crossinline factory: @DisallowComposableCalls () -> T,
): T = remember(tag) { factory() }

