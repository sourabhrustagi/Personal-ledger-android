package com.ledger.personal.domain.settings

/** Default category names from the static catalog. */
fun interface CategoryCatalog {
    /** Returns the catalog, or an empty list when it cannot be loaded. */
    suspend fun names(): List<String>
}
