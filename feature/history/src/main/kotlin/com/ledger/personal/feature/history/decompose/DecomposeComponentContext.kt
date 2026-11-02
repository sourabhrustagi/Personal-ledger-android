package com.ledger.personal.feature.history.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import java.util.concurrent.CopyOnWriteArrayList

/** Cancellation handle for Decompose Value subscription. */
fun interface DecomposeCancellation {
    fun cancel()
}

/**
 * Arkadii Ivanov's Decompose Value contract.
 * Represents an observable, retainable state holder independent of UI frameworks.
 */
interface Value<out T : Any> {
    val value: T
    fun subscribe(observer: (T) -> Unit): DecomposeCancellation
}

/** Mutable implementation of [Value]. */
class MutableValue<T : Any>(initialValue: T) : Value<T> {
    private val observers = CopyOnWriteArrayList<(T) -> Unit>()

    override var value: T = initialValue
        set(newValue) {
            field = newValue
            observers.forEach { it(newValue) }
        }

    override fun subscribe(observer: (T) -> Unit): DecomposeCancellation {
        observers.add(observer)
        observer(value)
        return DecomposeCancellation { observers.remove(observer) }
    }
}

/** Extension bridging Decompose [Value] to Compose [State]. */
@Composable
fun <T : Any> Value<T>.subscribeAsState(): State<T> {
    val state = remember(this) { mutableStateOf(value) }
    DisposableEffect(this) {
        val cancellation = subscribe { state.value = it }
        onDispose { cancellation.cancel() }
    }
    return state
}

/** Arkadii Ivanov's Decompose ComponentContext contract. */
interface ComponentContext {
    val key: String
}

/** Default lightweight ComponentContext. */
class DefaultComponentContext(override val key: String = "HistoryComponentContext") : ComponentContext
