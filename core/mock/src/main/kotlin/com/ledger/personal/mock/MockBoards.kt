package com.ledger.personal.mock

import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.HomeTile
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import javax.inject.Inject

/** Mock dashboard. The home screen draws these sections in this order and keeps no layout of its own. */
class MockHomeBoard @Inject constructor() : HomeBoard {
    /** Returns layout. */
    override suspend fun layout(): Result<HomeLayout> = MockSwitches.gate {
        Result.success(
            HomeLayout(
                tabs = listOf(
                    HomeTab("home", "Home"),
                    HomeTab("pay", "Pay"),
                    HomeTab("passbook", "Passbook"),
                    HomeTab("offers", "Offers"),
                ),
                sections = listOf(
                    HomeSection.Balance,
                ),
            ),
        )
    }
}

/** Mock payments team. */
class MockPaymentsBackend @Inject constructor() : PaymentsBackend {
    /** Returns shortcuts. */
    override suspend fun shortcuts(): Result<List<PayShortcut>> = MockSwitches.gate {
        Result.success(MockCatalog.contacts.map { PayShortcut(it.name, it.handle) })
    }
}



/** Mock promotions team. Tabs and copy come from here, not from the other mocks. */
class MockPromotionsBackend @Inject constructor() : PromotionsBackend {
    /** Returns tabs. */
    override suspend fun tabs(): Result<List<PromoTab>> = MockSwitches.gate {
        Result.success(
            listOf(
                PromoTab(
                    "festive",
                    "Festive",
                    listOf(
                        PromoOffer("upi5", "5% back on UPI up to ₹100", "Until 31 Oct"),
                        PromoOffer("grocery", "₹50 off groceries above ₹500", "Until 20 Oct"),
                    ),
                ),
                PromoTab(
                    "bills",
                    "Bills",
                    listOf(PromoOffer("electricity", "₹25 cashback on electricity", "This week")),
                ),
            ),
        )
    }
}
