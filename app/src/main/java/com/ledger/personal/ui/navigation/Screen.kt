package com.ledger.personal.ui.navigation

import kotlinx.serialization.Serializable

/** Contract for screen. */
sealed interface Screen {

    /** Route to the ledger history screen. */
    @Serializable
    data object LedgerHistory : Screen

    /** Route to the home screen. */
    @Serializable
    data object Home : Screen

    /** Route to the passbook screen. */
    @Serializable
    data object Passbook : Screen

    /** Route to the scan screen. */
    @Serializable
    data object Scan : Screen

    /** Route to the promotions/offers tab screen. */
    @Serializable
    data object Promotions : Screen

    /** Route to the account and security settings screen. */
    @Serializable
    data object Account : Screen
}
