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

/** Seed fraud alert. */
data class SeedFraudAlert(
    val id: String,
    val merchant: String,
    val location: String,
    val amountMinor: Long,
    val time: String,
    val reason: String,
    val score: Int = 0,
)

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

    val fraudAlerts: List<SeedFraudAlert> = listOf(
        SeedFraudAlert("card-1", "Electronics Hub", "Lagos, NG", 2_49_999_00L, "Today, 02:14", "New country", 94),
        SeedFraudAlert("card-2", "StreamPlus Subscription", "Online", 1_499_00L, "Today, 09:41", "Card not present", 41),
        SeedFraudAlert("card-3", "Fuel Station 24x7", "Pune, IN", 9_800_00L, "Yesterday, 23:55", "Unusual hour", 58),
        SeedFraudAlert("card-4", "Gift Cards Online", "Online", 50_000_00L, "Today, 03:02", "Rapid repeats", 88),
        SeedFraudAlert("card-5", "Jewel Palace", "Dubai, AE", 1_85_000_00L, "Today, 01:47", "New country", 91),
        SeedFraudAlert("card-6", "Metro Card Recharge", "Delhi, IN", 500_00L, "Today, 08:15", "Velocity", 22),
        SeedFraudAlert("card-7", "Crypto Exchange", "Online", 75_000_00L, "Today, 04:30", "High-risk merchant", 83),
        SeedFraudAlert("card-8", "Hotel Seaside", "Goa, IN", 18_400_00L, "Yesterday, 21:10", "Distance jump", 47),
        SeedFraudAlert("card-9", "ATM Withdrawal", "Bengaluru, IN", 40_000_00L, "Today, 00:58", "Unusual hour", 64),
        SeedFraudAlert("card-10", "App Store", "Online", 2_999_00L, "Today, 10:05", "Card not present", 18),
        SeedFraudAlert("card-11", "Luxury Watches", "Singapore, SG", 3_10_000_00L, "Today, 02:40", "New country", 97),
        SeedFraudAlert("card-12", "Food Delivery", "Mumbai, IN", 640_00L, "Today, 12:20", "Device change", 35),
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
