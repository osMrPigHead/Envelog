package cn.osmrpighead.envelog.ui.screen.envelope

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavId
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.TodayProvider
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
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
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult
import cn.osmrpighead.envelog.data.repository.EnvelopeUpdateResult
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
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
import kotlinx.coroutines.withTimeout
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
import java.time.DayOfWeek
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
    private val wholeMonth = ClosedDateSpan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31))
    private val afterExistingBudget = ClosedDateSpan(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 31))
    private val monthSpan = month.toHalfOpenDateSpan()
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
        assertEquals(listOf(monthSpan, budgetSpan), repository.requestedSpans)
    }

    @Test
    fun updateActiveBudget_requeriesWhenSpanChanges() = runTest {
        awaitSuccess()
        viewModel.openActiveBudget(budgetId)
        awaitAllocations()

        viewModel.updateActiveBudget(budget.copy(dateSpan = nextBudgetSpan))

        val allocations = awaitAllocations { it.contains(outside) && it.size == 1 }

        assertEquals(listOf(outside), allocations)
        assertEquals(listOf(monthSpan, budgetSpan, nextBudgetSpan), repository.requestedSpans)
    }

    @Test
    fun activeBudgetAllocations_areChronological() = runTest {
        awaitSuccess()
        viewModel.openActiveBudget(budgetId)

        val allocations = awaitAllocations()

        assertEquals(listOf(early, late), allocations)
    }

    @Test
    fun success_exposesMonthlyAllocationsChronologically() = runTest {
        val state = awaitSuccess()

        assertEquals(listOf(early, late, outside), state.allocationsWithInfo)
        assertEquals(listOf(monthSpan), repository.requestedSpans)
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
        assertEquals(listOf(monthSpan, budgetSpan), repository.requestedSpans)
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
            assertEquals(emptyList<AllocationWithInfo>(), state.allocationsWithInfo)
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun archiveAndBack_archivesEnvelopeThenBacks() = runTest {
        awaitSuccess()

        viewModel.archiveAndBack()

        assertEquals(listOf(envelopeId), repository.archivedIds)
        awaitBack(viewModel)
    }

    @Test
    fun archiveAndBack_onNewEnvelopeBacksWithoutArchiving() = runTest {
        val newViewModel = createViewModel(repository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.archiveAndBack()

            awaitBack(newViewModel)
        } finally {
            newViewModel.viewModelScope.cancel()
        }

        assertTrue(repository.archivedIds.isEmpty())
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
            newViewModel.applyQuickSet(QuickSetConfig.Daily(intervalDays = 1, amount = 1_000), wholeMonth)

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertEquals(31, state.budgets.size)
            assertTrue(state.budgets.values.all { it.amount == 1_000L })
            assertTrue(state.budgets.values.all { it.envelopeId == envelope.id })
            assertEquals(LocalDate.of(2026, 10, 1), state.budgets.values.minOf { it.dateSpan.startDate })
            assertEquals(LocalDate.of(2026, 10, 31), state.budgets.values.maxOf { it.dateSpan.startDate })
            assertTrue(state.quickSet.open)
            assertTrue(state.quickSet.complete)
            assertTrue(newRepository.replaceRequests.isEmpty())

            newViewModel.clearQuickSet()
            val cleared = newViewModel.uiState.value as EnvelopeUiState.Success
            assertFalse(cleared.quickSet.open)
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun applyQuickSet_weekly_createsWholeWeekBudgetsIncludingMostlyCoveredLastWeek() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(
                QuickSetConfig.Weekly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 1_000),
                wholeMonth,
            )

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertEquals(4, state.budgets.size)
            assertEquals(LocalDate.of(2026, 10, 5), state.budgets.values.minOf { it.dateSpan.startDate })
            assertEquals(LocalDate.of(2026, 11, 2), state.budgets.values.maxOf { it.dateSpan.endDate })
            assertTrue(state.budgets.values.all { it.amount == 1_000L })
            assertTrue(state.quickSet.complete)
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
            newViewModel.applyQuickSet(QuickSetConfig.Daily(intervalDays = 1, amount = 1_000), wholeMonth)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetConfig.MonthlyTotal(100_000), wholeMonth)

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertFalse(state.quickSet.complete)
            assertNotNull(state.quickSet.conflict)
            assertEquals(31, state.quickSet.conflict!!.conflicts.size)
            assertEquals(31, state.budgets.size)
            assertFalse(state.budgets.values.any { it.amount == 100_000L })
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun confirmQuickSetOverwrite_replacesBudgetsAndRequestsClose() = runTest {
        val newRepository = FakeRepository(
            envelope = Envelope(EnvelopeId.generate(), "新信封", true, false),
            budget = budget,
            allocations = emptyList(),
        )
        val newViewModel = createViewModel(newRepository, NavigationTarget.New())
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetConfig.Daily(intervalDays = 1, amount = 1_000), wholeMonth)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetConfig.MonthlyTotal(100_000), wholeMonth)
            newViewModel.confirmQuickSetOverwrite()

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertTrue(state.quickSet.complete)
            assertNull(state.quickSet.conflict)
            assertEquals(1, state.budgets.size)
            val budget = state.budgets.values.single()
            assertEquals(100_000L, budget.amount)
            assertEquals(LocalDate.of(2026, 10, 1), budget.dateSpan.startDate)
            assertEquals(LocalDate.of(2026, 11, 1), budget.dateSpan.endDate)
            assertTrue(newRepository.replaceRequests.isEmpty())

            newViewModel.clearQuickSet()
            val cleared = newViewModel.uiState.value as EnvelopeUiState.Success
            assertFalse(cleared.quickSet.open)
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
            newViewModel.applyQuickSet(QuickSetConfig.Daily(intervalDays = 1, amount = 1_000), wholeMonth)
            newViewModel.openQuickSet()
            newViewModel.applyQuickSet(QuickSetConfig.MonthlyTotal(100_000), wholeMonth)
            newViewModel.cancelQuickSetOverwrite()

            val state = newViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertFalse(state.quickSet.complete)
            assertNull(state.quickSet.conflict)
            assertEquals(31, state.budgets.size)
        } finally {
            newViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun applyQuickSet_onExistingEnvelope_usesReplaceBudgets() = runTest {
        awaitSuccess()

        viewModel.openQuickSet()
        viewModel.applyQuickSet(QuickSetConfig.Daily(intervalDays = 1, amount = 1_000), afterExistingBudget)

        val state = viewModel.uiState.value as EnvelopeUiState.Success
        assertEquals(1, repository.replaceRequests.size)
        val request = repository.replaceRequests.single()
        assertEquals(envelopeId, request.envelopeId)
        assertTrue(request.deleteBudgetIds.isEmpty())
        assertEquals(24, request.budgets.size)
        assertEquals(25, state.budgets.size)
        assertTrue(state.quickSet.open)
        assertTrue(state.quickSet.complete)
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
            archivedViewModel.applyQuickSet(
                QuickSetConfig.Daily(intervalDays = 1, amount = 1_000),
                afterExistingBudget,
            )

            val state = archivedViewModel.uiState.value as EnvelopeUiState.Success
            assertTrue(state.quickSet.open)
            assertFalse(state.quickSet.complete)
            assertEquals(QuickSetError.EnvelopeArchived, state.quickSet.error)
            assertNull(state.quickSet.conflict)
            assertEquals(1, state.budgets.size)
        } finally {
            archivedViewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun dismissQuickSet_requestsHideThenClearClearsState() = runTest {
        awaitSuccess()

        viewModel.openQuickSet()
        viewModel.dismissQuickSet()

        val closing = viewModel.uiState.value as EnvelopeUiState.Success
        assertTrue(closing.quickSet.open)
        assertTrue(closing.quickSet.complete)
        assertNull(closing.quickSet.conflict)
        assertNull(closing.quickSet.error)

        viewModel.clearQuickSet()

        val cleared = viewModel.uiState.value as EnvelopeUiState.Success
        assertFalse(cleared.quickSet.open)
        assertFalse(cleared.quickSet.complete)
        assertNull(cleared.quickSet.conflict)
        assertNull(cleared.quickSet.error)
    }

    private suspend fun awaitSuccess(): EnvelopeUiState.Success =
        viewModel.uiState.filterIsInstance<EnvelopeUiState.Success>().first()

    private suspend fun awaitBack(target: EnvelopeViewModel) =
        withTimeout(1_000) { target.backEvent.first() }

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
        val archivedIds = mutableListOf<EnvelopeId>()

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

        override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult {
            archivedIds += id
            return ArchiveResult.Archived
        }

        override suspend fun canDeleteEnvelope(id: EnvelopeId): Boolean =
            throw NotImplementedError()

        override suspend fun deleteEnvelope(id: EnvelopeId): EnvelopeDeleteResult =
            throw NotImplementedError()

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
