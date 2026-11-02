package com.ledger.personal.feature.history.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest

/**
 * CashApp Molecule Presenter.
 * Represents state machine logic expressed entirely as a @Composable function,
 * consuming an event Flow and producing an immutable [MoleculeHistoryState].
 */
@Composable
fun moleculeHistoryPresenter(
    repository: TransactionRepository,
    events: Flow<MoleculeHistoryEvent>,
): MoleculeHistoryState {
    var isLoading by remember { mutableStateOf(true) }
    var transactions by remember { mutableStateOf<List<HistoryTransactionItem>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTransaction by remember { mutableStateOf<HistoryTransactionItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository) {
        repository.observeTransactions()
            .catch { cause ->
                if (cause is CancellationException) throw cause
                isLoading = false
                errorMessage = cause.message ?: "Failed to observe transactions"
            }
            .collect { domainItems ->
                transactions = domainItems.map { it.toItem() }
                isLoading = false
                errorMessage = null
            }
    }

    LaunchedEffect(events) {
        events.collectLatest { event ->
            when (event) {
                is MoleculeHistoryEvent.SelectCategory -> {
                    selectedCategory = event.category
                }
                is MoleculeHistoryEvent.SelectTransaction -> {
                    selectedTransaction = event.transaction
                }
                is MoleculeHistoryEvent.DismissDetail -> {
                    selectedTransaction = null
                }
                is MoleculeHistoryEvent.Refresh -> {
                    isLoading = true
                    try {
                        repository.refresh()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Failed to refresh"
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }

    return MoleculeHistoryState(
        isLoading = isLoading,
        transactions = transactions,
        selectedCategory = selectedCategory,
        selectedTransaction = selectedTransaction,
        errorMessage = errorMessage,
    )
}

