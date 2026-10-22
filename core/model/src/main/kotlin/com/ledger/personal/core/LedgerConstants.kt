package com.ledger.personal.core

/**
 * Domain constants for currencies, categories, transactions, and business rules.
 */
object LedgerConstants {
    /** Default application currency (ISO 4217). */
    const val DEFAULT_CURRENCY: String = "INR"

    /** Indian Rupee currency code. */
    const val CURRENCY_INR: String = "INR"

    /** US Dollar currency code. */
    const val CURRENCY_USD: String = "USD"

    /** Indian Rupee currency symbol. */
    const val CURRENCY_SYMBOL_INR: String = "₹"

    /** Default primary salary category for credits. */
    const val CATEGORY_SALARY: String = "Salary"

    /** Standard expense categories. */
    const val CATEGORY_FOOD: String = "Food"
    const val CATEGORY_SHOPPING: String = "Shopping"
    const val CATEGORY_BILLS: String = "Bills"
    const val CATEGORY_ENTERTAINMENT: String = "Entertainment"
    const val CATEGORY_TRANSPORT: String = "Transport"
    const val CATEGORY_HEALTH: String = "Health"
    const val CATEGORY_INVESTMENTS: String = "Investments"
    const val CATEGORY_TRANSFER: String = "Transfer"
    const val CATEGORY_OTHER: String = "Other"

    /** Pagination default limit. */
    const val DEFAULT_PAGE_SIZE: Int = 25

    /** Standard recent transactions limit. */
    const val DEFAULT_RECENT_LIMIT: Int = 30
}
