package cn.osmrpighead.envelog.ui.screen.overview

import cn.osmrpighead.envelog.ui.screen.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import java.time.YearMonth

data class OverviewUiState(
    val yearMonth: YearMonth,
    val summaryItems: List<SummaryItem>,
    val envelopes: List<EnvelopeModel>,
)
