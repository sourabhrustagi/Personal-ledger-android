package com.ledger.personal.feature.promotions

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
    fun picksFirstTabByDefault() = runTest(testDispatcher) {
        val sampleTabs = listOf(
            PromoTab("food", "Dining", listOf(PromoOffer("d1", "20% off", "Tomorrow"))),
            PromoTab("travel", "Travel", emptyList()),
        )
        val fakeRepo = object : PromotionRepository {
            override fun observePromotions(): Flow<List<PromoTab>> = flowOf(sampleTabs)
        }
        val useCase = GetPromotionsUseCase(fakeRepo)
        val vm = PromotionsViewModel(getPromotionsUseCase = useCase)
        backgroundScope.launch { vm.uiState.collect {} }

        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("food", state.selected)
        assertEquals(2, state.shelves.size)
    }

    @Test
    fun selectTabUpdatesState() = runTest(testDispatcher) {
        val sampleTabs = listOf(
            PromoTab("food", "Dining", emptyList()),
            PromoTab("travel", "Travel", emptyList()),
        )
        val fakeRepo = object : PromotionRepository {
            override fun observePromotions(): Flow<List<PromoTab>> = flowOf(sampleTabs)
        }
        val useCase = GetPromotionsUseCase(fakeRepo)
        val vm = PromotionsViewModel(getPromotionsUseCase = useCase)
        backgroundScope.launch { vm.uiState.collect {} }

        testDispatcher.scheduler.advanceUntilIdle()
        vm.select("travel")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("travel", vm.uiState.value.selected)
    }

    @Test
    fun respectsSavedStateTab() = runTest(testDispatcher) {
        val sampleTabs = listOf(
            PromoTab("food", "Dining", emptyList()),
            PromoTab("travel", "Travel", emptyList()),
        )
        val fakeRepo = object : PromotionRepository {
            override fun observePromotions(): Flow<List<PromoTab>> = flowOf(sampleTabs)
        }
        val useCase = GetPromotionsUseCase(fakeRepo)
        val handle = SavedStateHandle(mapOf("tab" to "travel"))
        val vm = PromotionsViewModel(getPromotionsUseCase = useCase, handle = handle)
        backgroundScope.launch { vm.uiState.collect {} }

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("travel", vm.uiState.value.selected)
    }
}
