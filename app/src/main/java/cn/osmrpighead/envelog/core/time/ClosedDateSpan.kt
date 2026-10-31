package cn.osmrpighead.envelog.core.time

import java.time.LocalDate

data class ClosedDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun toHalfOpen() = HalfOpenDateSpan(startDate, endDate.plusDays(1))
}
