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
}
