package com.ledger.personal.feature.history.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Holder contract for CashApp Molecule style presenters.
 * Molecule converts composable presenters (@Composable () -> State) into reactive state streams.
 */
class MoleculePresenterScope<EVENT : Any> {
    val events = MutableSharedFlow<EVENT>(extraBufferCapacity = 64)

    fun sendEvent(event: EVENT) {
        events.tryEmit(event)
    }
}

/** Remembers a [MoleculePresenterScope] across recompositions. */
@Composable
fun <EVENT : Any> rememberMoleculeScope(): MoleculePresenterScope<EVENT> =
    remember { MoleculePresenterScope() }

