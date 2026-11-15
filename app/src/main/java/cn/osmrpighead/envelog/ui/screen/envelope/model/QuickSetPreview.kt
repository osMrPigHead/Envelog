package cn.osmrpighead.envelog.ui.screen.envelope.model

import java.time.DayOfWeek

sealed interface QuickSetPreview {
    data class Daily(
        val intervalDays: Long,
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
