package cn.osmrpighead.envelog.ui.screen.envelope

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavId
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.TodayProvider
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.DeleteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelopeUpdateResult
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class EnvelopeViewModelTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val today: LocalDate = LocalDate.of(2026, 10, 20)
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val clock: Clock = Clock.fixed(
        today.atTime(12, 0).atZone(zone).toInstant(),
        zone,
    )

    private val envelopeId = EnvelopeId(UUID(0L, 0xA1))
    private val budgetId = BudgetId(UUID(0L, 0xD1))
    private val budgetSpan = LocalDate.of(2026, 10, 1) untilExclusive LocalDate.of(2026, 10, 8)
    private val nextBudgetSpan = LocalDate.of(2026, 10, 8) untilExclusive LocalDate.of(2026, 10, 15)
    private val budget = Budget(
        id = budgetId,
        envelopeId = envelopeId,
        dateSpan = budgetSpan,
        attributedMonth = month,
        amount = 100_000,
    )

    private val early = allocation(0xC1, LocalDate.of(2026, 10, 3), 7_000)
    private val late = allocation(0xC2, LocalDate.of(2026, 10, 6), 5_000)
    private val outside = allocation(0xC3, LocalDate.of(2026, 10, 9), 9_000)

    private lateinit var repository: FakeRepository
    private lateinit var viewModel: EnvelopeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeRepository(
            envelope = Envelope(envelopeId, "餐饮", true, false),
            budget = budget,
            allocations = listOf(late, early, outside),
        )
        viewModel = createViewModel(repository)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun openActiveBudget_observesAllocationsWithinBudgetSpan() = runTest {
        awaitSuccess()

        viewModel.openActiveBudget(budgetId)

        val allocations = awaitAllocations()

        assertEquals(setOf(early, late), allocations.toSet())
        assertEquals(listOf(budgetSpan), repository.requestedSpans)
    }

    @Test
    fun updateActiveBudget_requeriesWhenSpanChanges() = runTest {
        awaitSuccess()
        viewModel.openActiveBudget(budgetId)
        awaitAllocations()

        viewModel.updateActiveBudget(budget.copy(dateSpan = nextBudgetSpan))

        val allocations = awaitAllocations { it.contains(outside) && it.size == 1 }

        assertEquals(listOf(outside), allocations)
        assertEquals(listOf(budgetSpan, nextBudgetSpan), repository.requestedSpans)
    }

    @Test
    fun activeBudgetAllocations_areChronological() = runTest {
        awaitSuccess()
        viewModel.openActiveBudget(budgetId)

        val allocations = awaitAllocations()

        assertEquals(listOf(early, late), allocations)
    }

    @Test
    fun clearActiveBudget_clearsAllocations() = runTest {
        awaitSuccess()
        viewModel.openActiveBudget(budgetId)
        awaitAllocations()

        viewModel.clearActiveBudget()

        val state = viewModel.uiState
            .filterIsInstance<EnvelopeUiState.Success>()
            .first { it.activeItem.budget == null }
        assertEquals(emptyList<AllocationWithInfo>(), state.activeItem.allocations)
        assertEquals(listOf(budgetSpan), repository.requestedSpans)
    }

    @Test
    fun newEnvelope_neverObservesAllocations() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = listOf(early, late),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.newActiveBudget(LocalDate.of(2026, 10, 5))

            assertTrue(newRepository.requestedSpans.isEmpty())
            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertEquals(emptyList<AllocationWithInfo>(), state.activeItem.allocations)
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun applyQuickSet_onNewEnvelope_createsDailyBudgetsInMemory() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            val envelope = Envelope(EnvelopeId.generate(), "新信封", true, false)
            newViewModel.initializeForNew(envelope)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.Daily, 1_000)

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertEquals(12, state.budgets.size)
            assertTrue(state.budgets.values.all { it.amount == 1_000L })
            assertTrue(state.budgets.values.all { it.envelopeId == envelope.id })
            assertEquals(LocalDate.of(2026, 10, 20), state.budgets.values.minOf { it.dateSpan.startDate })
            assertEquals(LocalDate.of(2026, 10, 31), state.budgets.values.maxOf { it.dateSpan.startDate })
            assertFalse(state.quickSet.open)
            assertTrue(newRepository.replaceRequests.isEmpty())
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun applyQuickSet_withConflict_keepsBudgetsAndSetsConflict() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.Daily, 1_000)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.MonthlyTotal, 100_000)

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertNotNull(state.quickSet.conflict)
            assertEquals(12, state.quickSet.conflict!!.conflicts.size)
            assertEquals(12, state.budgets.size)
            assertFalse(state.budgets.values.any { it.amount == 100_000L })
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun confirmQuickSetOverwrite_replacesBudgetsAndClosesSheet() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.Daily, 1_000)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.MonthlyTotal, 100_000)
            newViewModel.confirmQuickSetOverwrite()

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertFalse(state.quickSet.open)
            assertNull(state.quickSet.conflict)
            assertEquals(1, state.budgets.size)
            val budget = state.budgets.values.single()
            assertEquals(100_000L, budget.amount)
            assertEquals(LocalDate.of(2026, 10, 1), budget.dateSpan.startDate)
            assertEquals(LocalDate.of(2026, 11, 1), budget.dateSpan.endDate)
            assertTrue(newRepository.replaceRequests.isEmpty())
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun cancelQuickSetOverwrite_keepsSheetOpenAndBudgets() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.Daily, 1_000)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetMode.MonthlyTotal, 100_000)
            newViewModel.cancelQuickSetOverwrite()

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertNull(state.quickSet.conflict)
            assertEquals(12, state.budgets.size)
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun applyQuickSet_onExistingEnvelope_usesReplaceBudgets() = runTest {
        awaitSuccess()

        viewModel.openQuickSet()
        viewModel.applyQuickSet(QuickSetMode.Daily, 1_000)

        val state = viewModel.uiState.value as EnvelopeUiState.Success
        assertEquals(1, repository.replaceRequests.size)
        val request = repository.replaceRequests.single()
        assertEquals(envelopeId, request.envelopeId)
        assertTrue(request.deleteBudgetIds.isEmpty())
        assertEquals(12, request.budgets.size)
        assertEquals(13, state.budgets.size)
        assertFalse(state.quickSet.open)
    }

    @Test
    fun applyQuickSet_onExistingEnvelope_reportsArchivedError() = runTest {
        val archivedRepository = FakeRepository(
            envelope = Envelope(envelopeId, "餐饮", true, false),
            budget = budget,
            allocations = emptyList(),
            replaceResult = BudgetWriteResult.EnvelopeArchived,
        )
        val archivedViewModel = createViewModel(archivedRepository)
        try {
            archivedViewModel.uiState.filterIsInstance<EnvelopeUiState.Success>().first()
            archivedViewModel.openQuickSet()
            archivedViewModel.applyQuickSet(QuickSetMode.Daily, 1_000)

            val state = archivedViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertEquals(QuickSetError.EnvelopeArchived, state.quickSet.error)
            assertNull(state.quickSet.conflict)
            assertEquals(1, state.budgets.size)
        } finally {
            archivedViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun dismissQuickSet_clearsState() = runTest {
        awaitSuccess()

        viewModel.openQuickSet()
        viewModel.dismissQuickSet()

        val state = viewModel.uiState.value as EnvelopeUiState.Success
        assertFalse(state.quickSet.open)
        assertNull(state.quickSet.conflict)
        assertNull(state.quickSet.error)
    }

    private suspend fun awaitSuccess(): EnvelopeUiState.Success =
        viewModel.uiState.filterIsInstance<EnvelopeUiState.Success>().first()

    private suspend fun awaitAllocations(
        predicate: (List<AllocationWithInfo>) -> Boolean = { it.isNotEmpty() },
    ): List<AllocationWithInfo> = viewModel.uiState
        .filterIsInstance<EnvelopeUiState.Success>()
        .map { it.activeItem.allocations }
        .first(predicate)

    private fun createViewModel(
        repository: FakeRepository,
        target: NavigationTarget<EnvelopeId> = NavigationTarget.Existing(envelopeId),
    ): EnvelopeViewModel = EnvelopeViewModel(
        route = AppRoute.Envelope(navId = NavId.generate(), id = target),
        back = {},
        appState = AppStateHolder(
            todayProvider = TodayProvider(ApplicationProvider.getApplicationContext(), clock),
            clock = clock,
        ),
        repository = repository,
    )

    private fun allocation(value: Long, date: LocalDate, amount: Long): AllocationWithInfo =
        AllocationWithInfo(
            expenditure = ExpenditureMetaInfo(
                id = ExpenditureId(UUID(0L, value)),
                happenedAt = date.atTime(12, 0).atZone(zone).toInstant(),
                happenedDate = date,
                happenedZone = zone,
            ),
            envelopeId = envelopeId,
            amount = amount,
        )

    private class FakeRepository(
        private val envelope: Envelope,
        private val budget: Budget,
        private val allocations: List<AllocationWithInfo>,
        private val budgets: List<Budget> = listOf(budget),
        private val replaceResult: BudgetWriteResult = BudgetWriteResult.Success,
    ) : EnvelogRepository {
        data class ReplaceRequest(
            val envelopeId: EnvelopeId,
            val deleteBudgetIds: List<BudgetId>,
            val budgets: List<Budget>,
        )

        val requestedSpans = mutableListOf<HalfOpenDateSpan>()
        val replaceRequests = mutableListOf<ReplaceRequest>()

        override fun observeOverview(
            today: LocalDate,
            yearMonth: YearMonth,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<EnvelopeSummary>> = throw NotImplementedError()

        override fun observeAllocations(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<AllocationWithInfo>> {
            requestedSpans += dateSpan
            return flowOf(
                allocations.filter {
                    it.envelopeId == envelopeId && it.expenditure.happenedDate in dateSpan
                },
            )
        }

        override fun observeBudgets(
            envelopeId: EnvelopeId,
            attributedMonth: YearMonth,
        ): Flow<List<Budget>> = flowOf(budgets)

        override fun observeBudgets(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<Budget>> = flowOf(budgets)

        override fun observeDailyAllocationOverview(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<DailyAllocationSummary>> = flowOf(emptyList())

        override suspend fun getEnvelope(id: EnvelopeId): Envelope = envelope

        override suspend fun updateEnvelope(envelope: Envelope): EnvelopeUpdateResult =
            throw NotImplementedError()

        override suspend fun insertEnvelope(
            envelope: Envelope,
            budgets: List<Budget>,
        ): BudgetWriteResult = throw NotImplementedError()

        override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult = throw NotImplementedError()

        override suspend fun updateBudget(budget: Budget): BudgetWriteResult = throw NotImplementedError()

        override suspend fun insertBudget(budget: Budget): BudgetWriteResult = throw NotImplementedError()

        override suspend fun replaceBudgets(
            envelopeId: EnvelopeId,
            deleteBudgetIds: Collection<BudgetId>,
            budgets: List<Budget>,
        ): BudgetWriteResult {
            replaceRequests += ReplaceRequest(envelopeId, deleteBudgetIds.toList(), budgets)
            return replaceResult
        }

        override suspend fun deleteBudget(id: BudgetId): DeleteResult = throw NotImplementedError()

        override suspend fun recordExpenditure(expenditure: ExpenditureSubmission) =
            throw NotImplementedError()
    }
}
