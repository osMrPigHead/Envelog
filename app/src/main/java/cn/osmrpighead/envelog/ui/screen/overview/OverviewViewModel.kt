package cn.osmrpighead.envelog.ui.screen.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.number.ratioOf
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: User rule
private fun List<EnvelopeSummary>.toSummaryItems(): List<SummaryItem> {
    val allocated = sumOf { it.allocated }
    val budget = sumOf { if (it is EnvelopeSummary.Budgeted) it.totalBudget else 0 }
    return listOf(
        SummaryItem.Remaining(budget - allocated, allocated.ratioOf(budget)),
        SummaryItem.Expenses(allocated),
        SummaryItem.Balance(-allocated),  // TODO: +Income
    )
}

@HiltViewModel
class OverviewViewModel @Inject constructor(
    appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    private val today = appState.today

    @OptIn(ExperimentalCoroutinesApi::class)
    private val overview = combine(today, yearMonth) { today, yearMonth -> today to yearMonth }
        .flatMapLatest { (today, yearMonth) ->
            repository.observeOverview(
                today = today,
                yearMonth = yearMonth,
                dateSpan = yearMonth.toHalfOpenDateSpan(),  // TODO: User rule
            )
        }

    val uiState: StateFlow<OverviewUiState> = combine(today, yearMonth, overview) { today, yearMonth, overview ->
        OverviewUiState.Success(
            yearMonth = yearMonth,
            today = today,
            summaryItems = overview.toSummaryItems(),
            envelopes = overview,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverviewUiState.Loading)

    fun archiveEnvelope(id: EnvelopeId) = viewModelScope.launch { repository.archiveEnvelope(id) }
}
