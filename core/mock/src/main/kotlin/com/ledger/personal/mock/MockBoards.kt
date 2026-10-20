package com.ledger.personal.mock

import com.ledger.personal.domain.board.AssistantBackend
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.HomeTile
import com.ledger.personal.domain.board.InsightsBackend
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.board.SpendHighlight
import javax.inject.Inject

/** Mock dashboard. The home screen draws these sections in this order and keeps no layout of its own. */
class MockHomeBoard @Inject constructor() : HomeBoard {
    /** Returns layout. */
    override suspend fun layout(): Result<HomeLayout> = MockSwitches.gate {
        Result.success(
            HomeLayout(
                tabs = listOf(
                    HomeTab("home", "Home"),
                    HomeTab("money", "Money"),
                    HomeTab("pay", "Pay"),
                    HomeTab("insights", "Insights"),
                    HomeTab("offers", "Offers"),
                    HomeTab("assistant", "Assistant"),
                ),
                sections = listOf(
                    HomeSection.Balance,
                    HomeSection.Note("UPI cashback", "5% back until Sunday."),
                    HomeSection.Shortcuts(
                        listOf(
                            HomeTile("send", "Send"),
                            HomeTile("upi", "UPI"),
                            HomeTile("budgets", "Budgets"),
                            HomeTile("ask", "Ask"),
                            HomeTile("offers", "Offers"),
                        ),
                    ),
                    HomeSection.Activity("Latest activity"),
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

/** Mock insights team. */
class MockInsightsBackend @Inject constructor() : InsightsBackend {
    /** Returns highlights. */
    override suspend fun highlights(): Result<List<SpendHighlight>> = MockSwitches.gate {
        Result.success(listOf(SpendHighlight("Dining", 450_00L), SpendHighlight("Groceries", 1_842_50L)))
    }
}

/** Mock assistant team. */
class MockAssistantBackend @Inject constructor() : AssistantBackend {
    /** Returns prompts. */
    override suspend fun prompts(): Result<List<String>> = MockSwitches.gate {
        Result.success(
            listOf(
                "What's my balance?",
                "How much did I spend on groceries?",
                "What's my biggest expense?",
                "What's my loan EMI?",
            ),
        )
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
