package cn.osmrpighead.envelog.util.date

import cn.osmrpighead.envelog.common.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.FormatStyle
import java.util.Locale

data class DatePeriod(
    val start: LocalDate,
    val endExclusive: LocalDate,
) {
    val length: Long get() = start countDaysTo endExclusive

    operator fun contains(date: LocalDate): Boolean = date >= start && date < endExclusive

    operator fun iterator(): Iterator<LocalDate> = object : Iterator<LocalDate> {
        private var cur = start
        override fun hasNext(): Boolean = cur < endExclusive
        override fun next(): LocalDate {
            if (!hasNext()) throw NoSuchElementException()
            return cur.also { cur = cur.plusDays(1) }
        }
    }

    infix fun fullyBefore(other: DatePeriod): Boolean = endExclusive <= other.start
    infix fun hasBefore(other: DatePeriod): Boolean = start < other.start
    infix fun fullyAfter(other: DatePeriod): Boolean = start >= other.endExclusive
    infix fun hasAfter(other: DatePeriod): Boolean = endExclusive > other.endExclusive
    infix fun overlap(other: DatePeriod): Boolean = other.start < endExclusive && start < other.endExclusive
    infix fun intersect(other: DatePeriod): DatePeriod =
        maxOf(other.start, start) untilExclusive minOf(other.endExclusive, endExclusive)

    fun toClosed(): ClosedDatePeriod = ClosedDatePeriod(start, endExclusive.minusDays(1))

    fun isMajorityOf(other: DatePeriod, fallback: DatePeriod.(DatePeriod) -> Boolean): Boolean {
        val x = (2 * intersect(other).length).compareTo(other.length)
        return when {
            x > 0 -> true
            x < 0 -> false
            else -> fallback(other)
        }
    }

    fun splitIntoWeeks(firstDayOfWeek: DayOfWeek): List<DatePeriod> = buildList {
        var weekStart = start
        var weekEnd = start.nextOrSame(firstDayOfWeek)
        while (weekEnd < endExclusive) {
            add(weekStart untilExclusive weekEnd)
            weekStart = weekEnd
            weekEnd = weekEnd.plusDays(7)
        }
        add(weekStart untilExclusive endExclusive)
    }
}

data class ClosedDatePeriod(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    fun toHalfOpen(): DatePeriod = start untilExclusive endInclusive.plusDays(1)

    fun format(style: FormatStyle = FormatStyle.SHORT, locale: Locale = AppLocale.current()): String =
        "${start.format(style, locale)} ~ ${endInclusive.format(style, locale)}"
}
