package cn.osmrpighead.envelog.ui.overview.model

import cn.osmrpighead.envelog.util.Ratio

sealed class SummaryItem(val caption: Boolean) {
    data class Balance(val cents: Long): SummaryItem(caption = false)
    data class Expenses(val cents: Long): SummaryItem(caption = false)
    data class Remaining(val cents: Long, val usedProportion: Ratio): SummaryItem(caption = true)
}
