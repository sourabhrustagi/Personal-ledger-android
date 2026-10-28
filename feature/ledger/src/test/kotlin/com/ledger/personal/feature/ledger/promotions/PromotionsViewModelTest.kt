package com.ledger.personal.feature.ledger.promotions

import androidx.lifecycle.SavedStateHandle
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionRepository
import com.ledger.personal.domain.usecase.GetPromotionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PromotionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsPromotionsAndSelectsFirstShelfByDefault() = runTest(testDispatcher) {
        val sampleTabs = listOf(
            PromoTab("festive", "Festive", listOf(PromoOffer("upi5", "5% cashback", "Oct 31"))),
            PromoTab("bills", "Bills", listOf(PromoOffer("bill10", "10% off bills", "Nov 15"))),
        )
        val repo = object : PromotionRepository {
            override fun observePromotions(): Flow<List<PromoTab>> = flowOf(sampleTabs)
        }
        val viewModel = PromotionsViewModel(GetPromotionsUseCase(repo), SavedStateHandle())

        val testScope = TestScope(testDispatcher)
        val job = testScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.shelves.size)
        assertEquals("festive", viewModel.uiState.value.selected)

        viewModel.select("bills")
        advanceUntilIdle()

        assertEquals("bills", viewModel.uiState.value.selected)
        job.cancel()
    }

    @Test
    fun honorsRequestedTabFromSavedStateHandle() = runTest(testDispatcher) {
        val sampleTabs = listOf(
            PromoTab("festive", "Festive", emptyList()),
            PromoTab("bills", "Bills", emptyList()),
        )
        val repo = object : PromotionRepository {
            override fun observePromotions(): Flow<List<PromoTab>> = flowOf(sampleTabs)
        }
        val handle = SavedStateHandle(mapOf("tab" to "bills"))
        val viewModel = PromotionsViewModel(GetPromotionsUseCase(repo), handle)

        val testScope = TestScope(testDispatcher)
        val job = testScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertEquals("bills", viewModel.uiState.value.selected)
        job.cancel()
    }
}
