package com.ledger.personal.feature.history.orbit

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orbit MVI Container contract holding the immutable state and side-effect channel.
 */
interface Container<STATE : Any, SIDE_EFFECT : Any> {
    val stateFlow: StateFlow<STATE>
    val sideEffectFlow: Flow<SIDE_EFFECT>
    val scope: CoroutineScope

    suspend fun reduce(reducer: (STATE) -> STATE)
    suspend fun postSideEffect(sideEffect: SIDE_EFFECT)
}

/**
 * Host interface for components (such as ViewModels) that own an Orbit MVI [Container].
 */
interface ContainerHost<STATE : Any, SIDE_EFFECT : Any> {
    val container: Container<STATE, SIDE_EFFECT>
}

/** Context provided inside Orbit MVI's `intent { ... }` block. */
class SimpleSyntaxContext<STATE : Any, SIDE_EFFECT : Any>(
    private val containerInstance: Container<STATE, SIDE_EFFECT>,
) {
    val state: STATE get() = containerInstance.stateFlow.value

    suspend fun reduce(reducer: (STATE) -> STATE) {
        containerInstance.reduce(reducer)
    }

    suspend fun postSideEffect(sideEffect: SIDE_EFFECT) {
        containerInstance.postSideEffect(sideEffect)
    }
}

/** Concrete implementation of an Orbit MVI [Container]. */
class OrbitContainer<STATE : Any, SIDE_EFFECT : Any>(
    initialState: STATE,
    override val scope: CoroutineScope,
) : Container<STATE, SIDE_EFFECT> {
    private val mutex = Mutex()
    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<STATE> = _stateFlow.asStateFlow()

    private val _sideEffects = Channel<SIDE_EFFECT>(Channel.BUFFERED)
    override val sideEffectFlow: Flow<SIDE_EFFECT> = _sideEffects.receiveAsFlow()

    override suspend fun reduce(reducer: (STATE) -> STATE) {
        mutex.withLock {
            _stateFlow.value = reducer(_stateFlow.value)
        }
    }

    override suspend fun postSideEffect(sideEffect: SIDE_EFFECT) {
        _sideEffects.send(sideEffect)
    }
}

/** Helper function to dispatch an Orbit MVI intent from a [ContainerHost]. */
fun <STATE : Any, SIDE_EFFECT : Any> ContainerHost<STATE, SIDE_EFFECT>.intent(
    block: suspend SimpleSyntaxContext<STATE, SIDE_EFFECT>.() -> Unit,
) {
    container.scope.launch {
        try {
            val context = SimpleSyntaxContext(container)
            context.block()
        } catch (e: CancellationException) {
            throw e
        }
    }
}
