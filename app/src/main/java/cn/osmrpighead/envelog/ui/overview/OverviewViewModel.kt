package cn.osmrpighead.envelog.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.data.model.CurrencyType
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.overview.model.SummaryItem
import cn.osmrpighead.envelog.util.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth

private data class PeriodData(
    val start: LocalDate, val end: LocalDate,
    val envelopes: List<EnvelopeResult>,
    val transactions: List<Transaction>,
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
                    it.transaction.dateTime.toLocalDate() in currentBudget.startDate..<currentBudget.endDate
                }.sumOf { it.allocation.amount }
        val accBudget = envelope.budgets.filter { it.endDate <= today }.sumOf { it.amount }
        val accAllocation = envelope.allocations
            .filter { it.transaction.dateTime.toLocalDate() <= today }
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
    transactions.forEach { transaction ->
        when (transaction.type) {
            TransactionType.Expenditure -> {
                expenditure += transaction.amount
                balance -= transaction.amount
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

class OverviewViewModel(
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = MutableStateFlow(YearMonth.now())
    private val today = MutableStateFlow(LocalDate.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data: Flow<PeriodData> = yearMonth.flatMapLatest { yearMonth ->
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth().plusDays(1)
        combine(
            repository.observeActiveEnvelopesWithExtra(start, end),
            repository.observeTransactions(start, end),
        ) { envelopes, transactions -> PeriodData(start, end, envelopes, transactions) }
    }

    val uiState: StateFlow<OverviewUiState> =
        combine(yearMonth, today, data) { yearMonth, today, data -> data.toUiState(yearMonth, today) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            OverviewUiState(yearMonth.value, emptyList(), emptyList()),
        )
}
