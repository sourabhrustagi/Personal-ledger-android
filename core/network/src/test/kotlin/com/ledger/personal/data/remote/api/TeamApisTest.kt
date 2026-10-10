package com.ledger.personal.data.remote.api

import com.ledger.personal.domain.board.HomeSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamApisTest {
    @Test
    fun eachTeamDropsBlankRowsAndKeepsItsOwnFields() {
        assertNull(TileDto(id = "", title = "Send").toHomeTile())
        assertEquals("send", TileDto("send", "Send").toHomeTile()?.id)

        assertNull(PayeeDto(displayName = "Asha", vpa = "").toShortcut())
        assertEquals("asha@ok", PayeeDto("Asha", "asha@ok").toShortcut()?.handle)

        assertNull(HighlightDto(label = "", spentPaise = 10).toHighlight())
        assertEquals(450_00L, HighlightDto("Dining", 450_00L).toHighlight()?.spentMinor)

        assertNull(ShelfDto(key = "", name = "Festive").toTab())
        val shelf = ShelfDto(
            "festive",
            "Festive",
            listOf(OfferDto(sku = "", pitch = "skip"), OfferDto("upi5", "5% back", "31 Oct")),
        ).toTab()
        assertEquals("festive", shelf?.id)
        assertEquals(listOf("upi5"), shelf?.offers?.map { it.id })

        assertNull(TabDto(id = "", title = "Home").toTab())
        assertEquals("home", TabDto("home", "Home").toTab()?.id)
        assertEquals(
            listOf("home"),
            DashboardDto(tabs = listOf(TabDto("", "Nope"), TabDto("home", "Home"))).toLayout().tabs.map { it.id },
        )

        assertNull(SectionDto(kind = "mystery", title = "Nope").toSection())
        assertNull(SectionDto(kind = "note", title = "Empty").toSection())
        assertNull(SectionDto(kind = "shortcuts", items = listOf(TileDto("", "Send"))).toSection())
        assertTrue(SectionDto(kind = "balance").toSection() is HomeSection.Balance)
        val note = SectionDto(kind = "note", title = "UPI cashback", body = "5% back").toSection()
        assertEquals("5% back", (note as HomeSection.Note).body)
        val shortcuts = SectionDto(
            kind = "shortcuts",
            items = listOf(TileDto("", "Nope"), TileDto("send", "Send")),
        ).toSection() as HomeSection.Shortcuts
        assertEquals(listOf("send"), shortcuts.tiles.map { it.id })
    }
}
