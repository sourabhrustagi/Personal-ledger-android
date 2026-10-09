package com.ledger.personal.ui.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object TransactionList : Screen

    @Serializable
    data class TransactionDetail(val transactionId: String) : Screen

    @Serializable
    data object CreateTransaction : Screen

    @Serializable
    data class EditTransaction(val transactionId: String) : Screen

    @Serializable
    data object Charts : Screen

    @Serializable
    data object LedgerHistory : Screen

    @Serializable
    data object FxAlerts : Screen

    @Serializable
    data object Kyc : Screen

    @Serializable
    data object Wallet : Screen

    @Serializable
    data object Ticker : Screen

    @Serializable
    data object Assistant : Screen

    @Serializable
    data object Banking : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object MoneyHub : Screen

    @Serializable
    data object PayHub : Screen

    @Serializable
    data object InsightsHub : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Security : Screen

    @Serializable
    data object Notifications : Screen

    @Serializable
    data object MockControls : Screen

    @Serializable
    data object Help : Screen

    @Serializable
    data object Cards : Screen

    @Serializable
    data object Loans : Screen

    @Serializable
    data object Deposits : Screen

    @Serializable
    data object Budgets : Screen

    @Serializable
    data object Statements : Screen

    @Serializable
    data object Categories : Screen

    @Serializable
    data class Dispute(val transactionId: String) : Screen

    @Serializable
    data object Disputes : Screen

    @Serializable
    data object Conflicts : Screen

    @Serializable
    data object Scan : Screen

    @Serializable
    data class PayTo(val handle: String, val amountMinor: Long) : Screen

    @Serializable
    data class SplitBill(val amountMinor: Long = 0L, val note: String = "") : Screen
}
