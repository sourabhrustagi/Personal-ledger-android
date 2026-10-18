package com.ledger.personal.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySyncTest {
    @Test
    fun remoteCatalogFillsAnEmptyDeviceAndKeepsASavedList() {
        val remote = listOf("Rent", "Salary")
        assertEquals(CategoryLists(remote, remote), categoriesAfterSync(stored = null, savedDefaults = null, remote = remote))
        assertEquals(
            CategoryLists(remote, listOf("Groceries")),
            categoriesAfterSync(stored = listOf("Groceries"), savedDefaults = null, remote = remote),
        )
    }

    @Test
    fun anEmptyResponseDoesNotInventCategories() {
        assertEquals(CategoryLists(emptyList(), emptyList()), categoriesAfterSync(null, null, emptyList()))
        val saved = listOf("Rent")
        assertEquals(CategoryLists(saved, listOf("Groceries")), categoriesAfterSync(listOf("Groceries"), saved, emptyList()))
    }
}
