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

    /** Route to the create transaction screen. */
    @Serializable
    data object CreateTransaction : Screen

    /** Route to the edit transaction screen. */
    @Serializable
    data class EditTransaction(val transactionId: String) : Screen

    /** Route to the charts screen. */
    @Serializable
    data object Charts : Screen

    /** Route to the ledger history screen. */
    @Serializable
    data object LedgerHistory : Screen

    /** Route to the fx alerts screen. */
    @Serializable
    data object FxAlerts : Screen

    /** Route to the kyc screen. */
    @Serializable
    data object Kyc : Screen

    /** Route to the wallet screen. */
    @Serializable
    data object Wallet : Screen

    /** Route to the ticker screen. */
    @Serializable
    data object Ticker : Screen

    /** Route to the banking screen. */
    @Serializable
    data object Banking : Screen

    /** Route to the home screen. */
    @Serializable
    data object Home : Screen

    /** Route to the money hub screen. */
    @Serializable
    data object MoneyHub : Screen

    /** Route to the pay hub screen. */
    @Serializable
    data object PayHub : Screen

    /** Route to the profile screen. */
    @Serializable
    data object Profile : Screen

    /** Route to the settings screen. */
    @Serializable
    data object Settings : Screen

    /** Route to the security screen. */
    @Serializable
    data object Security : Screen

    /** Route to the notifications screen. */
    @Serializable
    data object Notifications : Screen

    /** Route to the mock controls screen. */
    @Serializable
    data object MockControls : Screen

    /** Route to the help screen. */
    @Serializable
    data object Help : Screen

    /** Route to the cards screen. */
    @Serializable
    data object Cards : Screen

    /** Route to the loans screen. */
    @Serializable
    data object Loans : Screen

    /** Route to the deposits screen. */
    @Serializable
    data object Deposits : Screen

    /** Route to the budgets screen. */
    @Serializable
    data object Budgets : Screen

    /** Route to the statements screen. */
    @Serializable
    data object Statements : Screen

    /** Route to the categories screen. */
    @Serializable
    data object Categories : Screen

    /** Route to the dispute screen. */
    @Serializable
    data class Dispute(val transactionId: String) : Screen

    /** Route to the disputes screen. */
    @Serializable
    data object Disputes : Screen

    /** Route to the conflicts screen. */
    @Serializable
    data object Conflicts : Screen

    /** Route to the scan screen. */
    @Serializable
    data object Scan : Screen

    /** Route to the pay to screen. */
    @Serializable
    data class PayTo(val handle: String, val amountMinor: Long) : Screen

    /** Route to the split bill screen. */
    @Serializable
    data class SplitBill(val amountMinor: Long = 0L, val note: String = "") : Screen

    /** Offers. [tab] selects a shelf key from the promotions API. Blank opens the first shelf. */
    @Serializable
    data class Promotions(val tab: String = "") : Screen
}
