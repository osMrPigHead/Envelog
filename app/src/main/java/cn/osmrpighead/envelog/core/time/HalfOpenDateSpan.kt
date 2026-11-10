package cn.osmrpighead.envelog.core.time

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class HalfOpenDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val length get() = startDate countDaysTo endDate
    val attributedMonth: YearMonth
        get() = // TODO: User rule
            if (startDate.yearMonth == endDate.yearMonth) startDate.yearMonth
            else if ((startDate.yearMonth.toHalfOpenDateSpan() intersect this).length > length / 2) startDate.yearMonth
            else if ((endDate.yearMonth.toHalfOpenDateSpan() intersect this).length > length / 2) endDate.yearMonth
            else startDate.yearMonth

    fun splitIntoWeeks(firstDayOfWeek: DayOfWeek): List<HalfOpenDateSpan> = buildList {
        var weekStart = startDate
        var weekEnd = startDate.next(firstDayOfWeek)
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

    infix fun overlaps(other: HalfOpenDateSpan): Boolean = startDate < other.endDate && other.startDate < endDate

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

    operator fun contains(date: LocalDate): Boolean = date in startDate..<endDate

    fun toClosed() = ClosedDateSpan(startDate, endDate.minusDays(1))
}

fun Collection<HalfOpenDateSpan>.hasOverlap(): Boolean {
    val sorted = filter { it.startDate < it.endDate }.sortedBy { it.startDate }
    for (i in 1 until sorted.size) if (sorted[i - 1].endDate > sorted[i].startDate) return true
    return false
}
