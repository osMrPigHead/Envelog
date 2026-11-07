package cn.osmrpighead.envelog.core.time

import java.time.DayOfWeek
import java.time.LocalDate

data class HalfOpenDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val length get() = startDate countDaysTo endDate

    fun splitIntoWeeks(firstDayOfWeek: DayOfWeek): List<HalfOpenDateSpan> = buildList {
        var weekStart = startDate
        var weekEnd = startDate.nextOrSame(firstDayOfWeek)
        while (weekEnd < endDate) {
            add(weekStart untilExclusive weekEnd)
            weekStart = weekEnd
            weekEnd = weekEnd.plusDays(7)
        }
        add(weekStart untilExclusive endDate)
    }

    infix fun intersect(other: HalfOpenDateSpan): HalfOpenDateSpan = HalfOpenDateSpan(
        maxOf(startDate, other.startDate),
        minOf(endDate, other.endDate),
    )

    infix fun overlaps(other: HalfOpenDateSpan): Boolean = startDate < other.endDate && endDate < other.startDate

    infix fun hasBefore(other: HalfOpenDateSpan): Boolean = startDate < other.startDate
    infix fun hasAfter(other: HalfOpenDateSpan): Boolean = endDate > other.endDate

    operator fun iterator(): Iterator<LocalDate> = object : Iterator<LocalDate> {
        private var cur = startDate
        override fun hasNext(): Boolean = cur < endDate
        override fun next(): LocalDate {
            if (!hasNext()) throw NoSuchElementException()
            return cur.also { cur = cur.plusDays(1) }
        }
    }

    fun toClosed() = ClosedDateSpan(startDate, endDate.minusDays(1))
}
