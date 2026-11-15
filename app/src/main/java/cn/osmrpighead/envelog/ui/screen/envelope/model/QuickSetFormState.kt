package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.ClosedDateSpan

data class QuickSetFormState(
    val mode: QuickSetMode,
    val dailyConfig: QuickSetConfig.Daily,
    val weeklyConfig: QuickSetConfig.Weekly,
    val monthlyConfig: QuickSetConfig.Monthly,
    val targetSpan: ClosedDateSpan,
) {
    val selectedConfig: QuickSetConfig = when (mode) {
        QuickSetMode.Daily -> dailyConfig
        QuickSetMode.Weekly -> weeklyConfig
        QuickSetMode.Monthly -> monthlyConfig
    }
}
