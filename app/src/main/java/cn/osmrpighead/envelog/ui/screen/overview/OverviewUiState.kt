package cn.osmrpighead.envelog.ui.screen.overview

import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.overview.model.HeadlineSummary
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import java.time.LocalDate
import java.time.YearMonth

data class OverviewUiState(
    val yearMonth: YearMonth,
    val today: LocalDate,
    val loadState: LoadState,
    val expenseFormState: FormState<Unit>,
)

sealed interface LoadState {
    data class Success(
        val headlineSummary: HeadlineSummary,
        val viceSummary: List<SummaryItem>,
        val envelopes: List<EnvelopeSummary>,
    ) : LoadState

    data object Loading : LoadState
}
