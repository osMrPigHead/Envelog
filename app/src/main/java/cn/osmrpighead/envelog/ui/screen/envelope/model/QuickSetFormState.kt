package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.ClosedDateSpan

data class QuickSetFormState(
    val mode: QuickSetMode,
    val dailyConfig: QuickSetConfig.Daily,
    val weeklyConfig: QuickSetConfig.Weekly,
    val monthlyConfig: QuickSetConfig.MonthlyTotal,
    val targetSpan: ClosedDateSpan,
)
