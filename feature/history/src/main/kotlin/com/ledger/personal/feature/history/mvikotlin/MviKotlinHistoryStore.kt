package com.ledger.personal.feature.history.mvikotlin

import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Factory to construct an MVIKotlin Store with Executor, Reducer, and Bootstrapper. */
object MviKotlinHistoryStoreFactory {

    fun create(
        repository: TransactionRepository,
        scope: CoroutineScope,
    ): Store<MviKotlinHistoryIntent, MviKotlinHistoryState, MviKotlinHistoryLabel> {
        val bootstrapper = Bootstrapper<MviKotlinHistoryAction> { dispatch ->
            dispatch(MviKotlinHistoryAction.Init)
        }

        val executor = object : Executor<
            MviKotlinHistoryIntent,
            MviKotlinHistoryAction,
            MviKotlinHistoryState,
            MviKotlinHistoryMessage,
            MviKotlinHistoryLabel,
            > {
            private lateinit var coroutineScope: CoroutineScope
            private lateinit var dispatchMessage: (MviKotlinHistoryMessage) -> Unit
            private lateinit var publishLabel: (MviKotlinHistoryLabel) -> Unit

            override fun init(
                scope: CoroutineScope,
                stateProvider: () -> MviKotlinHistoryState,
                dispatchMessage: (MviKotlinHistoryMessage) -> Unit,
                publishLabel: (MviKotlinHistoryLabel) -> Unit,
            ) {
                this.coroutineScope = scope
                this.dispatchMessage = dispatchMessage
                this.publishLabel = publishLabel
            }

            override fun executeAction(action: MviKotlinHistoryAction) {
                when (action) {
                    is MviKotlinHistoryAction.Init -> loadTransactions()
                }
            }

            override fun executeIntent(intent: MviKotlinHistoryIntent) {
                when (intent) {
                    is MviKotlinHistoryIntent.ChangeCategory -> {
                        dispatchMessage(MviKotlinHistoryMessage.SetCategory(intent.category))
                    }
                    is MviKotlinHistoryIntent.SelectTransaction -> {
                        dispatchMessage(MviKotlinHistoryMessage.SetSelected(intent.transaction))
                    }
                    is MviKotlinHistoryIntent.DismissDetail -> {
                        dispatchMessage(MviKotlinHistoryMessage.SetSelected(null))
                    }
                    is MviKotlinHistoryIntent.Refresh -> refresh()
                }
            }

            private fun loadTransactions() {
                coroutineScope.launch {
                    dispatchMessage(MviKotlinHistoryMessage.SetLoading(true))
                    repository.observeTransactions()
                        .catch { cause ->
                            if (cause is CancellationException) throw cause
                            dispatchMessage(MviKotlinHistoryMessage.SetLoading(false))
                            dispatchMessage(
                                MviKotlinHistoryMessage.SetError(
                                    cause.message ?: "Failed to load transactions",
                                ),
                            )
                        }
                        .collect { domainItems ->
                            dispatchMessage(MviKotlinHistoryMessage.SetLoading(false))
                            dispatchMessage(
                                MviKotlinHistoryMessage.SetTransactions(
                                    domainItems.map { it.toItem() },
                                ),
                            )
                        }
                }
            }

            private fun refresh() {
                coroutineScope.launch {
                    dispatchMessage(MviKotlinHistoryMessage.SetLoading(true))
                    try {
                        repository.refresh()
                        publishLabel(MviKotlinHistoryLabel.Toast("Refreshed"))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        dispatchMessage(MviKotlinHistoryMessage.SetError(e.message ?: "Refresh failed"))
                    } finally {
                        dispatchMessage(MviKotlinHistoryMessage.SetLoading(false))
                    }
                }
            }
        }

        val reducer = Reducer<MviKotlinHistoryState, MviKotlinHistoryMessage> { state, message ->
            when (message) {
                is MviKotlinHistoryMessage.SetLoading -> state.copy(isLoading = message.isLoading)
                is MviKotlinHistoryMessage.SetTransactions -> state.copy(
                    transactions = message.items,
                    errorMessage = null,
                )
                is MviKotlinHistoryMessage.SetCategory -> state.copy(selectedCategory = message.category)
                is MviKotlinHistoryMessage.SetSelected -> state.copy(selectedTransaction = message.transaction)
                is MviKotlinHistoryMessage.SetError -> state.copy(errorMessage = message.errorMessage)
            }
        }

        return DefaultStore(
            initialState = MviKotlinHistoryState(isLoading = true),
            scope = scope,
            bootstrapper = bootstrapper,
            executor = executor,
            reducer = reducer,
        )
    }
}
