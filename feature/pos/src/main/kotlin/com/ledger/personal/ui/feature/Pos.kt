package com.ledger.personal.ui.feature

import com.ledger.personal.domain.pay.FetchedBill
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedBiller
import com.ledger.personal.mock.SeedCard

/** Steps of bill payment. */
enum class PosStep { BILLS, ACCOUNT, CARD, PIN }

/** Values rendered by the bill payment screen. */
data class PosUiState(
    val billers: List<SeedBiller> = MockCatalog.billers,
    val cards: List<SeedCard> = MockCatalog.cards,
    val paidBillers: Set<String> = emptySet(),
    val biller: SeedBiller? = null,
    val card: SeedCard? = null,
    val step: PosStep = PosStep.BILLS,
    val billNumber: String = "",
    val fetched: FetchedBill? = null,
    val fetching: Boolean = false,
    val fetchFailed: Boolean = false,
    val bill: String? = null,
    val paying: Boolean = false,
    val errorMessage: String? = null,
)
