package cn.osmrpighead.envelog.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.util.formatAmount
import cn.osmrpighead.envelog.util.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

sealed interface BudgetScope {
    data object Today : BudgetScope
}

enum class SummaryType {
    Remaining,
    Expenses,
    Balance,
}

enum class CurrencyType {
    CNY,
}

data class BudgetRelated(
    val usedPercent: String,
    val totalAmount: String,
    val availableFor: BudgetScope,
    val availableNow: String,
    val availableDiff: String,
)

data class EnvelopeModel(
    val id: UUID,
    val name: String,
    val usedAmount: String,
    val currency: CurrencyType,
    val diffNeg: Boolean,
    val budgetRelated: BudgetRelated?,
)

data class SummaryItem(
    val type: SummaryType,
    val value: String,
    val captionValue: String? = null,
)

class OverviewViewModel(
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = MutableStateFlow(YearMonth.now())
    private val today = MutableStateFlow(LocalDate.now())

    private data class PeriodData(
        val start: LocalDate, val end: LocalDate,
        val envelopes: List<EnvelopeResult>,
        val transactions: List<Transaction>,
    )

    private data class EnvelopeSummary(
        val envelope: EnvelopeResult,
        val budget: Long?,
        val allocation: Long,
    )

    data class UiState(
        val yearMonth: YearMonth,
        val summaryItems: List<SummaryItem>,
        val envelopes: List<EnvelopeModel>,
    )

    private fun EnvelopeSummary.toEnvelopeModel(today: LocalDate): EnvelopeModel {
        var budgetRelated: BudgetRelated? = null
        var diffNeg = false
        if (envelope.budgets != null) {
            val availableNow =
                envelope.budgets.filter { today in it.startDate..<it.endDate }
                    .sumOf { it.amount } -
                        envelope.allocations.filter { it.transaction.dateTime.toLocalDate() == today }
                            .sumOf { it.allocations[0].amount }
            val availableDiff =
                envelope.budgets.filter { it.startDate < today }
                    .sumOf { it.amount } -
                        envelope.allocations.filter { it.transaction.dateTime.toLocalDate() < today }
                            .sumOf { it.allocations[0].amount }  // TODO: User rule
            diffNeg = availableDiff < 0
            budgetRelated = BudgetRelated(
                usedPercent = allocation.percent(budget!!),
                totalAmount = budget.formatAmount(),
                availableFor = BudgetScope.Today,  // TODO
                availableNow = availableNow.formatAmount(0),
                availableDiff = availableDiff.formatAmount(0, true),
            )
        }
        return EnvelopeModel(
            id = envelope.envelope.id,
            name = envelope.envelope.name,
            usedAmount = allocation.formatAmount(),
            currency = CurrencyType.CNY,
            diffNeg = diffNeg,
            budgetRelated = budgetRelated,
        )
    }

    private fun PeriodData.toUiState(yearMonth: YearMonth, today: LocalDate): UiState {
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
                budget = it.budgets?.fold(0L) { acc, budget -> acc + budget.amount },
                allocation = it.allocations.fold(0L) { acc, allocation -> acc + allocation.allocations[0].amount },
            )
        }
        val budget = summaries.fold(0L) { acc, summary -> acc + (summary.budget ?: 0) }
        val allocation = summaries.fold(0L) { acc, summary -> acc + summary.allocation }

        return UiState(
            yearMonth = yearMonth,
            summaryItems = listOf(
                SummaryItem(
                    type = SummaryType.Remaining,
                    value = (budget - allocation).formatAmount(),
                    captionValue = allocation.percent(budget),
                ),
                SummaryItem(
                    type = SummaryType.Expenses,
                    value = expenditure.formatAmount(),
                ),
                SummaryItem(
                    type = SummaryType.Balance,
                    value = balance.formatAmount(),
                ),
            ),
            envelopes = summaries.map { it.toEnvelopeModel(today) },
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val data: Flow<PeriodData> = yearMonth.flatMapLatest { yearMonth ->
        val start = yearMonth.atDay(1)
        val end = yearMonth.atEndOfMonth().plusDays(1)
        combine(
            repository.observeActiveEnvelopesWithExtra(start, end),
            repository.observeTransactions(start, end),
        ) { envelopes, transactions -> PeriodData(start, end, envelopes, transactions) }
    }

    private val uiState: StateFlow<UiState> =
        combine(yearMonth, today, data) { yearMonth, today, data -> data.toUiState(yearMonth, today) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiState(yearMonth.value, emptyList(), emptyList()),
        )
}
