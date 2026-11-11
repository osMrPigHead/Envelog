package cn.osmrpighead.envelog.ui.screen.envelope

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavId
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
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
import cn.osmrpighead.envelog.data.repository.EnvelopeUpdateResult
import cn.osmrpighead.envelog.ui.model.AppStateHolder
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
    private var backCalls = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeRepository(
            envelope = Envelope(envelopeId, "餐饮", true, false),
            budget = budget,
            allocations = listOf(late, early, outside),
        )
        viewModel = createViewModel(repository) { backCalls++ }
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
        assertEquals(1, backCalls)
    }

    @Test
    fun archiveAndBack_onNewEnvelopeBacksWithoutArchiving() = runTest {
        var newBackCalls = 0

        val newViewModel = createViewModel(repository, NavigationTarget.New()) { newBackCalls++ }
        try {
            newViewModel.initializeForNew(Envelope(EnvelopeId.generate(), "新信封", true, false))
            newViewModel.archiveAndBack()
        } finally {
            newViewModel.viewModelScope.cancel()
        }

        assertTrue(repository.archivedIds.isEmpty())
        assertEquals(1, newBackCalls)
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
        onBack: () -> Unit = {},
    ): EnvelopeViewModel = EnvelopeViewModel(
        route = AppRoute.Envelope(navId = NavId.generate(), id = target),
        back = onBack,
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
    ) : EnvelogRepository {
        val requestedSpans = mutableListOf<HalfOpenDateSpan>()
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
        ): Flow<List<Budget>> = flowOf(listOf(budget))

        override fun observeBudgets(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<Budget>> = flowOf(listOf(budget))

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

        override suspend fun updateBudget(budget: Budget): BudgetWriteResult = throw NotImplementedError()

        override suspend fun insertBudget(budget: Budget): BudgetWriteResult = throw NotImplementedError()

        override suspend fun deleteBudget(id: BudgetId): DeleteResult = throw NotImplementedError()

        override suspend fun recordExpenditure(expenditure: ExpenditureSubmission) =
            throw NotImplementedError()
    }
}
