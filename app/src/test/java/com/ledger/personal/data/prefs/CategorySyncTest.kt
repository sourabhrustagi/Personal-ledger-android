package com.ledger.personal.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySyncTest {
    @Test
    fun remoteCatalogFillsAnEmptyDeviceAndKeepsASavedList() {
        val remote = listOf("Rent", "Salary")
        assertEquals(remote to remote, categoriesAfterSync(stored = null, savedDefaults = null, remote = remote))
        assertEquals(
            remote to listOf("Groceries"),
            categoriesAfterSync(stored = listOf("Groceries"), savedDefaults = null, remote = remote),
        )
    }

    @Test
    fun anEmptyResponseDoesNotInventCategories() {
        assertEquals(emptyList<String>() to emptyList<String>(), categoriesAfterSync(null, null, emptyList()))
        val saved = listOf("Rent")
        assertEquals(saved to listOf("Groceries"), categoriesAfterSync(listOf("Groceries"), saved, emptyList()))
    }
}
