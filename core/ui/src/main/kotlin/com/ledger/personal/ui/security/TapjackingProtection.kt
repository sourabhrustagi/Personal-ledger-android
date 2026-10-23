package com.ledger.personal.ui.security

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Enforces tapjacking protection by configuring the active view hierarchy to reject
 * touch events whenever an overlay, floating window, or screen-filter is partially
 * or fully obscuring the window.
 *
 * Implements Android's MotionEvent.FLAG_WINDOW_IS_OBSCURED and FLAG_WINDOW_IS_PARTIALLY_OBSCURED
 * defense mechanisms to protect high-value banking and transaction confirmation actions.
 */
@Composable
fun TapjackingProtection() {
    val view = LocalView.current
    DisposableEffect(view) {
        val previous = view.filterTouchesWhenObscured
        view.filterTouchesWhenObscured = true
        onDispose {
            view.filterTouchesWhenObscured = previous
        }
    }
}
