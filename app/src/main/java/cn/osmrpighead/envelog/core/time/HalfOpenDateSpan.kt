package cn.osmrpighead.envelog.core.time

import java.time.LocalDate

data class HalfOpenDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun toClosed() = ClosedDateSpan(startDate, endDate.minusDays(1))
}
