package com.ledger.personal.ui.navigation

import kotlinx.serialization.Serializable

/** Contract for screen. */
sealed interface Screen {
    /** Route to the transaction list screen. */
    @Serializable
    data object TransactionList : Screen

    /** Route to the transaction detail screen. */
    @Serializable
    data class TransactionDetail(val transactionId: String) : Screen


    /** Route to the ledger history screen. */
    @Serializable
    data object LedgerHistory : Screen

    /** Route to the wallet screen. */
    @Serializable
    data object Wallet : Screen

    /** Route to the home screen. */
    @Serializable
    data object Home : Screen

    /** Route to the pay hub screen. */
    @Serializable
    data object PayHub : Screen

    /** Route to the passbook screen. */
    @Serializable
    data object Passbook : Screen

    /** Route to the scan screen. */
    @Serializable
    data object Scan : Screen

    /** Route to the pay to screen. */
    @Serializable
    data class PayTo(val handle: String, val amountMinor: Long) : Screen

    /** Offers. [tab] selects a shelf key from the promotions API. Blank opens the first shelf. */
    @Serializable
    data class Promotions(val tab: String = "") : Screen
}
