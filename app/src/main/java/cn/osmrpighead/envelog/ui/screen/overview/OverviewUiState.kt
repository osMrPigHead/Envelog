package cn.osmrpighead.envelog.ui.screen.overview

import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import java.time.LocalDate
import java.time.YearMonth

sealed interface OverviewUiState {
    data class Success(
        val yearMonth: YearMonth,
        val today: LocalDate,
        val summaryItems: List<SummaryItem>,
        val envelopes: List<EnvelopeSummary>,
    ) : OverviewUiState

    data object Loading : OverviewUiState
}
