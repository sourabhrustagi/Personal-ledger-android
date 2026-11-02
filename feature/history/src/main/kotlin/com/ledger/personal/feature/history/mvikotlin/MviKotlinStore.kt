package com.ledger.personal.feature.history.mvikotlin

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
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
 * Arkadii Ivanov's MVIKotlin Store contract.
 * Pure MVI with strict unidirectional separation between Intent, Message, and State.
 */
interface Store<in Intent : Any, out State : Any, out Label : Any> {
    val stateFlow: StateFlow<State>
    val labelFlow: Flow<Label>
    fun accept(intent: Intent)
    fun dispose()
}

/** Pure reducer interface: maps (State, Message) -> State. */
fun interface Reducer<State : Any, in Message : Any> {
    fun reduce(state: State, message: Message): State
}

/** Bootstrapper producing initial Actions upon store creation. */
fun interface Bootstrapper<out Action : Any> {
    fun bootstrap(dispatch: (Action) -> Unit)
}

/** Executor handling business logic and producing Messages for Reducer and Labels. */
interface Executor<in Intent : Any, in Action : Any, State : Any, out Message : Any, out Label : Any> {
    fun init(
        scope: CoroutineScope,
        stateProvider: () -> State,
        dispatchMessage: (Message) -> Unit,
        publishLabel: (Label) -> Unit,
    )
    fun executeIntent(intent: Intent)
    fun executeAction(action: Action)
}

/** Default implementation of Arkadii Ivanov's MVIKotlin [Store]. */
class DefaultStore<Intent : Any, Action : Any, Message : Any, State : Any, Label : Any>(
    initialState: State,
    private val scope: CoroutineScope,
    private val bootstrapper: Bootstrapper<Action>?,
    private val executor: Executor<Intent, Action, State, Message, Label>,
    private val reducer: Reducer<State, Message>,
) : Store<Intent, State, Label> {

    private val mutex = Mutex()
    private val _stateFlow = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<State> = _stateFlow.asStateFlow()

    private val _labelChannel = Channel<Label>(Channel.BUFFERED)
    override val labelFlow: Flow<Label> = _labelChannel.receiveAsFlow()

    init {
        executor.init(
            scope = scope,
            stateProvider = { _stateFlow.value },
            dispatchMessage = { message ->
                scope.launch {
                    mutex.withLock {
                        _stateFlow.value = reducer.reduce(_stateFlow.value, message)
                    }
                }
            },
            publishLabel = { label ->
                scope.launch {
                    try {
                        _labelChannel.send(label)
                    } catch (e: CancellationException) {
                        throw e
                    }
                }
            },
        )
        bootstrapper?.bootstrap { action ->
            executor.executeAction(action)
        }
    }

    override fun accept(intent: Intent) {
        executor.executeIntent(intent)
    }

    override fun dispose() {
        scope.cancel()
    }
}
