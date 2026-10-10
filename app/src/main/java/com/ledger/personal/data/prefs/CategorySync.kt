package com.ledger.personal.data.prefs

import com.ledger.personal.domain.settings.CategoryCatalog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The catalog is whatever the static API returned.
 * A list the user already saved is kept. An empty response does not invent names.
 */
fun categoriesAfterSync(
    stored: List<String>?,
    savedDefaults: List<String>?,
    remote: List<String>,
): Pair<List<String>, List<String>> {
    val incoming = remote.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val defaults = incoming.ifEmpty { savedDefaults.orEmpty() }
    return defaults to (stored ?: defaults)
}

/** Loads default categories from GET static/categories and does not show a list before that call finishes. */
@Singleton
class CategorySync @Inject constructor(
    private val catalog: CategoryCatalog,
    private val store: AppPreferences,
) {
    /** Pulls the catalog and saves it. */
    suspend fun pull() {
        val remote = catalog.names()
        val (defaults, selected) = categoriesAfterSync(store.storedCategories(), store.storedDefaults(), remote)
        store.saveCategorySync(defaults, selected)
    }
}
