package cn.osmrpighead.envelog.ui.screen.envelope.model

import java.time.DayOfWeek

sealed interface QuickSetPreview {
    val days: Int
    val totalAmount: Long

    data class Daily(
        val intervalDays: Int,
        val amount: Long,
        override val days: Int,
        override val totalAmount: Long,
    ) : QuickSetPreview

    data class Weekly(
        val startDayOfWeek: DayOfWeek,
        val endDayOfWeek: DayOfWeek,
        val amount: Long,
        override val days: Int,
        override val totalAmount: Long,
    ) : QuickSetPreview

    data class MonthlyTotal(
        override val days: Int,
        override val totalAmount: Long,
    ) : QuickSetPreview
}
