package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import java.time.DayOfWeek

internal sealed interface QuickSetPreview {
    data class Daily(
        val intervalDays: Int,
        val amount: Long,
        val days: Long,
        val totalAmount: Long,
    ) : QuickSetPreview

    data class Weekly(
        val startDayOfWeek: DayOfWeek,
        val endDayOfWeek: DayOfWeek,
        val amount: Long,
        val days: Long,
        val totalAmount: Long,
    ) : QuickSetPreview

    data class MonthlyTotal(
        val totalAmount: Long,
        val days: Long,
    ) : QuickSetPreview
}
