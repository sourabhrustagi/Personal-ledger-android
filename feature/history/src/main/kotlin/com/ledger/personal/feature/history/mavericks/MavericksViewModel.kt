package com.ledger.personal.feature.history.mavericks

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Airbnb Mavericks State marker interface. */
interface MavericksState

/**
 * Airbnb Mavericks sealed class representing the state of an asynchronous operation.
 */
sealed class Async<out T>(open val value: T? = null) {
    data object Uninitialized : Async<Nothing>()
    data class Loading<out T>(override val value: T? = null) : Async<T>(value)
    data class Success<out T>(override val value: T) : Async<T>(value)
    data class Fail<out T>(val error: Throwable, override val value: T? = null) : Async<T>(value)

    operator fun invoke(): T? = value
}

/**
 * Airbnb Mavericks ViewModel managing state updates via [setState],
 * asynchronous execution via [execute], and synchronous inspection via [withState].
 */
abstract class MavericksViewModel<S : MavericksState>(
    initialState: S,
    protected val viewModelScope: CoroutineScope,
) {
    private val _stateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    fun setState(reducer: S.() -> S) {
        _stateFlow.update { it.reducer() }
    }

    fun <R> withState(action: (S) -> R): R = action(_stateFlow.value)

    fun <T> Flow<T>.execute(
        reducer: S.(Async<T>) -> S,
    ): Job {
        setState { reducer(Async.Loading(value = null)) }
        return viewModelScope.launch {
            this@execute
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    setState { reducer(Async.Fail(cause)) }
                }
                .collect { item ->
                    setState { reducer(Async.Success(item)) }
                }
        }
    }

    fun <T> (suspend () -> T).execute(
        reducer: S.(Async<T>) -> S,
    ): Job {
        setState { reducer(Async.Loading(value = null)) }
        return viewModelScope.launch {
            try {
                val result = this@execute()
                setState { reducer(Async.Success(result)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                setState { reducer(Async.Fail(e)) }
            }
        }
    }
}
