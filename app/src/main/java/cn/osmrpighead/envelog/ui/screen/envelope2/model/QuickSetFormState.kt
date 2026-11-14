package cn.osmrpighead.envelog.ui.screen.envelope2.model

import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode

data class QuickSetFormState(
    val mode: QuickSetMode,
    val dailyConfig: QuickSetConfig.Daily,
    val weeklyConfig: QuickSetConfig.Weekly,
    val monthlyConfig: QuickSetConfig.MonthlyTotal,
    val targetSpan: ClosedDateSpan,
)
