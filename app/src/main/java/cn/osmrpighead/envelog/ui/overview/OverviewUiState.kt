package cn.osmrpighead.envelog.ui.overview

import cn.osmrpighead.envelog.ui.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.overview.model.SummaryItem
import java.time.YearMonth

data class OverviewUiState(
    val yearMonth: YearMonth,
    val summaryItems: List<SummaryItem>,
    val envelopes: List<EnvelopeModel>,
)
