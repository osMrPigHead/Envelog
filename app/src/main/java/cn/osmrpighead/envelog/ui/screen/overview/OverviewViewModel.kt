package cn.osmrpighead.envelog.ui.screen.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.number.ratioOf
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.overview.model.Balance
import cn.osmrpighead.envelog.ui.screen.overview.model.Expenses
import cn.osmrpighead.envelog.ui.screen.overview.model.HeadlineSummary
import cn.osmrpighead.envelog.ui.screen.overview.model.Remaining
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

// TODO: User rule
private fun List<EnvelopeSummary>.toSummaryItems(): Pair<HeadlineSummary, List<SummaryItem>> {
    val budgetedAllocated = sumOf { if (it is EnvelopeSummary.Budgeted && !it.archived) it.allocated else 0 }
    val allocated = sumOf { it.allocated }
    val budget = sumOf { if (it is EnvelopeSummary.Budgeted && !it.archived) it.totalBudget else 0 }
    return Pair(
        Remaining(budget - budgetedAllocated, budget, budgetedAllocated.ratioOf(budget)),
        listOf(Expenses(allocated), Balance(-allocated)), // TODO: +Income
    )
}

@HiltViewModel
class OverviewViewModel @Inject constructor(
    private val appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    private val today = appState.today

    fun selectMonth(yearMonth: YearMonth) = appState.selectMonth(yearMonth)

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
    private val loadState = combine(today, yearMonth) { today, yearMonth -> today to yearMonth }
        .flatMapLatest { (today, yearMonth) ->
            repository.observeOverview(
                today = today,
                yearMonth = yearMonth,
                dateSpan = yearMonth.toHalfOpenDateSpan(),  // TODO: User rule
            ).map { overview ->
                val (headline, vice) = overview.toSummaryItems()
                LoadState.Success(
                    headlineSummary = headline,
                    viceSummary = vice,
                    envelopes = overview.filter { !it.archived },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LoadState.Loading)

    private val expenseFormState = MutableStateFlow<FormState<Unit>>(FormState.Active(Unit))

    val uiState: StateFlow<OverviewUiState> =
        combine(today, yearMonth, loadState, expenseFormState) { today, yearMonth, loadState, expenseFormState ->
            OverviewUiState(yearMonth, today, loadState, expenseFormState)
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            OverviewUiState(
                yearMonth.value, today.value,
                LoadState.Loading, expenseFormState.value,
            ),
        )

    fun archiveEnvelope(id: EnvelopeId) = viewModelScope.launch { repository.archiveEnvelope(id) }
    fun deleteEnvelope(id: EnvelopeId) = viewModelScope.launch { repository.deleteEnvelope(id) }

    fun requireExpenseFormState() {
        expenseFormState.value = FormState.Active(Unit)
    }

    fun submitExpense(submission: ExpenseSubmission) {
        if (expenseFormState.value !is FormState.Active) return
        expenseFormState.value = FormState.Busy
        viewModelScope.launch {
            repository.recordExpense(submission)
            expenseFormState.value = FormState.Completed
        }
    }
}
