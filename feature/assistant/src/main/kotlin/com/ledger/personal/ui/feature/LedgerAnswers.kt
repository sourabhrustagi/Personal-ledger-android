package com.ledger.personal.ui.feature

import com.ledger.personal.core.emiMinor
import com.ledger.personal.mock.MockCatalog

val assistantSuggestions = listOf(
    "What's my balance?",
    "How much did I spend on groceries?",
    "What's my biggest expense?",
    "What's my loan EMI?",
)

fun ledgerAnswer(question: String): String {
    val q = question.lowercase()
    val inr = MockCatalog.transactions.filter { it.currency == "INR" }
    val spending = inr.filter { it.category != "Salary" }
    val category = MockCatalog.transactions.map { it.category }.distinct().firstOrNull { q.contains(it.lowercase()) }
    return when {
        q.contains("balance") -> {
            val accounts = MockCatalog.bankAccounts.joinToString { "${it.name} ${formatMoney(it.balanceMinor)}" }
            "You have ${formatMoney(MockCatalog.bankAccounts.sumOf { it.balanceMinor })} across your accounts: $accounts."
        }
        q.contains("emi") || q.contains("loan") ->
            "Your loan of ${formatMoney(MockCatalog.loanPrincipalMinor)} at 12% for 36 months has an EMI of " +
                "${formatMoney(emiMinor(MockCatalog.loanPrincipalMinor, 1_200, 36))} a month."
        category != null -> {
            val rows = MockCatalog.transactions.filter { it.category == category }
            val byCurrency = rows.groupBy { it.currency }.entries.joinToString(" and ") { (currency, items) ->
                formatMoney(items.sumOf { it.amountMinor }, currency)
            }
            "You spent $byCurrency on $category across ${rows.size} transactions."
        }
        q.contains("biggest") || q.contains("top") || q.contains("most") -> {
            val top = spending.groupBy { it.category }.maxByOrNull { (_, items) -> items.sumOf { it.amountMinor } }
            top?.let { (name, items) -> "Your biggest expense is $name at ${formatMoney(items.sumOf { it.amountMinor })}." }
                ?: "I couldn't find any spending yet."
        }
        q.contains("spend") || q.contains("spent") || q.contains("expense") ->
            "You've spent ${formatMoney(spending.sumOf { it.amountMinor })} from your rupee account across ${spending.size} transactions."
        else -> "I can answer questions about your balance, spending by category, and your loan EMI. " +
            "Try \"How much did I spend on groceries?\""
    }
}
