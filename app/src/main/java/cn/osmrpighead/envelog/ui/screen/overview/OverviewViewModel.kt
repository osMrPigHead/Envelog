package cn.osmrpighead.envelog.ui.screen.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.data.db.entity.CurrencyType
import cn.osmrpighead.envelog.data.db.entity.EntryEntity
import cn.osmrpighead.envelog.data.db.entity.EntryType
import cn.osmrpighead.envelog.data.query_old.DateRange
import cn.osmrpighead.envelog.data.query_old.DateRangeRule
import cn.osmrpighead.envelog.data.query_old.EnvelopeResult
import cn.osmrpighead.envelog.data.repository_old.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.model.SummaryItem
import cn.osmrpighead.envelog.core.time.first
import cn.osmrpighead.envelog.core.time.next
import cn.osmrpighead.envelog.util.percent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

private data class PeriodData(
    val start: LocalDate, val end: LocalDate,
    val envelopes: List<EnvelopeResult>,
    val transactions: List<EntryEntity>,
)

private data class EnvelopeSummary(
    val envelope: EnvelopeResult,
    val budget: Long,
    val allocation: Long,
)

private fun EnvelopeSummary.toEnvelopeModel(today: LocalDate): EnvelopeModel {
    var budgetRelated: EnvelopeModel.BudgetRelated? = null
    if (envelope.envelope.isBudgeted) {
        val currentBudget = envelope.budgets.find { today in it.startDate..<it.endDate }
        val currentAllocation =
            if (currentBudget == null) 0L
            else envelope.allocations
                .filter {
                    it.transaction.happenedAt.toLocalDate() in currentBudget.startDate..<currentBudget.endDate
                }.sumOf { it.allocation.amount }
        val accBudget = envelope.budgets.filter { it.endDate <= today }.sumOf { it.amount }
        val accAllocation = envelope.allocations
            .filter { it.transaction.happenedAt.toLocalDate() <= today }
            .sumOf { it.allocation.amount } - currentAllocation
        budgetRelated = EnvelopeModel.BudgetRelated(
            budgetAmount = budget,
            usedProportion = allocation.percent(budget),
            endDate = currentBudget?.endDate,
            today = today,
            currentAvailable = (currentBudget?.amount ?: 0L) - currentAllocation,
            diffAmount = accBudget - accAllocation,  // TODO: User rule
        )
    }
    return EnvelopeModel(
        id = envelope.envelope.id,
        name = envelope.envelope.name,
        currency = CurrencyType.CNY,
        usedAmount = allocation,
        budgetRelated = budgetRelated,
    )
}

private fun PeriodData.toUiState(yearMonth: YearMonth, today: LocalDate): OverviewUiState {
    var expenditure = 0L
    var balance = 0L
    for ((_, type, _, amount) in transactions) {
        when (type) {
            EntryType.Expenditure -> {
                expenditure += amount
                balance -= amount
            }
        }
    }
    val summaries = envelopes.map {
        EnvelopeSummary(
            envelope = it,
            budget = it.budgets.fold(0L) { acc, budget -> acc + budget.amount },
            allocation = it.allocations.fold(0L) { acc, allocation -> acc + allocation.allocation.amount },
        )
    }
    val budget = summaries.fold(0L) { acc, summary -> acc + summary.budget }
    val allocation = summaries.fold(0L) { acc, summary -> acc + summary.allocation }

    return OverviewUiState(
        yearMonth = yearMonth,
        summaryItems = listOf(
            SummaryItem.Remaining(budget - allocation, allocation.percent(budget)),
            SummaryItem.Expenses(expenditure),
            SummaryItem.Balance(balance),
        ),
        envelopes = summaries.map { it.toEnvelopeModel(today) },
    )
}

@HiltViewModel
class OverviewViewModel @Inject constructor(
    appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    private val today = appState.today

    init {
        appState.updateMonthIfFollowing()
        viewModelScope.launch {
            var lastMonth = YearMonth.from(appState.today.value)
            appState.today.collect { today ->
                val currentMonth = YearMonth.from(today)
                if (lastMonth != currentMonth) {
                    lastMonth = currentMonth
                    appState.updateMonthIfFollowing()
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data: Flow<PeriodData> = yearMonth.flatMapLatest { yearMonth ->
        val start = yearMonth.first
        val end = yearMonth.next.first
        val range = DateRange(start = start, end = end, rule = DateRangeRule.StartInRange)
        combine(
            repository.observeActiveEnvelopesWithExtra(range),
            repository.observeTransactions(range),
        ) { envelopes, transactions -> PeriodData(start, end, envelopes, transactions) }
    }

    val uiState: StateFlow<OverviewUiState> =
        combine(yearMonth, today, data) { yearMonth, today, data -> data.toUiState(yearMonth, today) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OverviewUiState(yearMonth.value, emptyList(), emptyList()),
        )

    fun archiveEnvelope(id: UUID) = runBlocking { repository.archiveEnvelope(id) }
}
