package com.ledger.personal.feature.history.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Slack Circuit Presenter producing [CircuitHistoryUiState]. */
class CircuitHistoryPresenter(
    private val repository: TransactionRepository,
) : CircuitPresenter<CircuitHistoryUiState> {

    @Composable
    override fun present(): CircuitHistoryUiState {
        var isLoading by remember { mutableStateOf(true) }
        var transactions by remember { mutableStateOf<List<HistoryTransactionItem>>(emptyList()) }
        var selectedCategory by remember { mutableStateOf<String?>(null) }
        var selectedTransaction by remember { mutableStateOf<HistoryTransactionItem?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(repository) {
            repository.observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    isLoading = false
                    errorMessage = cause.message ?: "Failed to load transactions"
                }
                .collect { domainItems ->
                    transactions = domainItems.map { it.toItem() }
                    isLoading = false
                    errorMessage = null
                }
        }

        return CircuitHistoryUiState(
            isLoading = isLoading,
            transactions = transactions,
            selectedCategory = selectedCategory,
            selectedTransaction = selectedTransaction,
            errorMessage = errorMessage,
            eventSink = { event ->
                when (event) {
                    is CircuitHistoryEvent.ChangeFilter -> {
                        selectedCategory = event.category
                    }
                    is CircuitHistoryEvent.SelectTransaction -> {
                        selectedTransaction = event.transaction
                    }
                    is CircuitHistoryEvent.DismissDetail -> {
                        selectedTransaction = null
                    }
                    is CircuitHistoryEvent.Refresh -> {
                        scope.launch {
                            isLoading = true
                            try {
                                repository.refresh()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Refresh failed"
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
        )
    }
}

