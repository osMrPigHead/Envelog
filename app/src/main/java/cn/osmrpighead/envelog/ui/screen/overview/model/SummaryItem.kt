package cn.osmrpighead.envelog.ui.screen.overview.model

import cn.osmrpighead.envelog.core.number.Ratio

sealed interface SummaryItem
sealed interface HeadlineSummary

data class Balance(val cents: Long): SummaryItem
data class Expenses(val cents: Long): SummaryItem
data class Remaining(val cents: Long, val total: Long, val usedProportion: Ratio): SummaryItem, HeadlineSummary
