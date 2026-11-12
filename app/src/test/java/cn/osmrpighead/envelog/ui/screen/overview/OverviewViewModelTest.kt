package cn.osmrpighead.envelog.ui.screen.overview

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.TodayProvider
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.DeleteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class OverviewViewModelTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val today: LocalDate = LocalDate.of(2026, 10, 20)
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val clock: Clock = Clock.fixed(
        today.atTime(12, 0).atZone(zone).toInstant(),
        zone,
    )

    private val activeEnvelopeId = EnvelopeId(UUID(0L, 0xA1))
    private val archivedEnvelopeId = EnvelopeId(UUID(0L, 0xA2))
    private val shoppingEnvelopeId = EnvelopeId(UUID(0L, 0xA3))

    private lateinit var viewModel: OverviewViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val application = ApplicationProvider.getApplicationContext<Application>()
        val appState = AppStateHolder(
            todayProvider = TodayProvider(application, clock),
            clock = clock,
        )
        viewModel = OverviewViewModel(
            appState = appState,
            repository = FakeRepository(
                listOf(
                    budgeted(
                        id = activeEnvelopeId,
                        name = "餐饮",
                        allocated = 10_000,
                        archived = false,
                        totalBudget = 100_000,
                    ),
                    budgeted(
                        id = archivedEnvelopeId,
                        name = "旧信封",
                        allocated = 7_000,
                        archived = true,
                        totalBudget = 50_000,
                    ),
                    EnvelopeSummary.NonBudget(
                        id = shoppingEnvelopeId,
                        name = "购物",
                        allocated = 3_000,
                        currency = CurrencyType.CNY,
                        archived = false,
                    ),
                ),
            ),
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun archivedEnvelope_isHiddenFromEnvelopeList() = runTest {
        val state = successState()

        assertEquals(
            listOf(activeEnvelopeId, shoppingEnvelopeId),
            state.envelopes.map { it.id },
        )
    }

    @Test
    fun archivedEnvelopeBudget_isExcludedFromSummaryButItsExpenditureCountsInTotal() = runTest {
        val state = successState()
        val remaining = state.summaryItems.filterIsInstance<SummaryItem.Remaining>().single()
        val expenses = state.summaryItems.filterIsInstance<SummaryItem.Expenses>().single()
        val balance = state.summaryItems.filterIsInstance<SummaryItem.Balance>().single()

        // 归档预算 50000 不计入总预算；归档信封的 7000 支出仍计入总支出。
        assertEquals(83_000L, remaining.cents)
        assertEquals(20_000L, expenses.cents)
        assertEquals(-20_000L, balance.cents)
        assertEquals(0, BigDecimal("0.17").compareTo(remaining.usedProportion.ratio))
    }

    private suspend fun successState(): OverviewUiState.Success =
        viewModel.uiState.filterIsInstance<OverviewUiState.Success>().first()

    private fun budgeted(
        id: EnvelopeId,
        name: String,
        allocated: Long,
        archived: Boolean,
        totalBudget: Long,
    ): EnvelopeSummary.Budgeted = EnvelopeSummary.Budgeted(
        id = id,
        name = name,
        allocated = allocated,
        currency = CurrencyType.CNY,
        archived = archived,
        totalBudget = totalBudget,
        currentBudget = totalBudget,
        currentSpan = month.toHalfOpenDateSpan(),
        currentSpanAllocated = allocated,
        priorBudget = 0,
        priorAllocated = 0,
    )

    private class FakeRepository(
        private val summaries: List<EnvelopeSummary>,
    ) : EnvelogRepository {
        override fun observeOverview(
            today: LocalDate,
            yearMonth: YearMonth,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<EnvelopeSummary>> = flowOf(summaries)

        override fun observeAllocations(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<AllocationWithInfo>> = emptyFlow()

        override fun observeBudgets(
            envelopeId: EnvelopeId,
            attributedMonth: YearMonth,
        ): Flow<List<Budget>> = emptyFlow()

        override fun observeBudgets(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<Budget>> =
            throw NotImplementedError()

        override fun observeDailyAllocationOverview(
            envelopeId: EnvelopeId,
            dateSpan: HalfOpenDateSpan,
        ): Flow<List<DailyAllocationSummary>> = emptyFlow()

        override suspend fun getEnvelope(id: EnvelopeId): Envelope =
            throw NotImplementedError()

        override suspend fun updateEnvelope(envelope: Envelope) =
            throw NotImplementedError()

        override suspend fun insertEnvelope(
            envelope: Envelope,
            budgets: List<Budget>,
        ): BudgetWriteResult = throw NotImplementedError()

        override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult =
            throw NotImplementedError()

        override suspend fun deleteEnvelope(id: EnvelopeId): EnvelopeDeleteResult =
            throw NotImplementedError()

        override suspend fun updateBudget(budget: Budget): BudgetWriteResult =
            throw NotImplementedError()

        override suspend fun insertBudget(budget: Budget): BudgetWriteResult =
            throw NotImplementedError()

        override suspend fun replaceBudgets(
            envelopeId: EnvelopeId,
            deleteBudgetIds: Collection<BudgetId>,
            budgets: List<Budget>,
        ): BudgetWriteResult = throw NotImplementedError()

        override suspend fun deleteBudget(id: BudgetId): DeleteResult =
            throw NotImplementedError()

        override suspend fun recordExpenditure(expenditure: ExpenditureSubmission) =
            throw NotImplementedError()
    }
}
