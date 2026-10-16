package com.ledger.personal.mock

/** Seed transaction. */
data class SeedTransaction(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val category: String,
    val dayOffset: Int,
)

/** Seed account. */
data class SeedAccount(val name: String, val lastFour: String, val balanceMinor: Long)

/** Seed contact. */
data class SeedContact(val name: String, val handle: String)

/** Seed biller. */
data class SeedBiller(val id: String, val name: String, val dueMinor: Long, val dueDate: String)

/** Seed card. Last four only; the PAN never sits in the catalog. */
data class SeedCard(val id: String, val name: String, val lastFour: String, val network: String)

/** Shared mock catalog. */
object MockCatalog {
    val bankAccounts: List<SeedAccount> = listOf(
        SeedAccount("Savings", "4821", 2_45_678_90L),
        SeedAccount("Salary", "1093", 86_420_15L),
        SeedAccount("Fixed deposit", "7710", 5_00_000_00L),
    )

    val contacts: List<SeedContact> = listOf(
        SeedContact("Asha Rao", "asha@okaxis"),
        SeedContact("Rahul Mehta", "rahul.m@okicici"),
        SeedContact("Priya Shah", "priya@oksbi"),
        SeedContact("Vikram Iyer", "vikram@okhdfc"),
    )

    val billers: List<SeedBiller> = listOf(
        SeedBiller("electricity", "Electricity · BESCOM", 50_000L, "Due 14 Oct"),
        SeedBiller("mobile", "Mobile postpaid · Airtel", 79_900L, "Due 18 Oct"),
        SeedBiller("broadband", "Broadband · ACT Fibernet", 1_17_900L, "Due 21 Oct"),
        SeedBiller("water", "Water · BWSSB", 32_000L, "Due 25 Oct"),
    )

    val cards: List<SeedCard> = listOf(
        SeedCard("debit-4821", "Savings debit", "4821", "Visa"),
        SeedCard("credit-9034", "Rewards credit", "9034", "RuPay"),
    )

    private val categories = listOf("Groceries", "Dining", "Transport", "Salary", "Utilities", "Entertainment")

    val transactions: List<SeedTransaction> = List(200) { index ->
        SeedTransaction(
            id = "tx-$index",
            amountMinor = 100L * (index + 1),
            currency = if (index % 2 == 0) "INR" else "USD",
            category = categories[index % categories.size],
            dayOffset = index,
        )
    }

    val accounts: List<String> = listOf("cash", "checking", "savings", "wallet", "card", "loan")

    val loanPrincipalMinor: Long = 50_000_000L
}
