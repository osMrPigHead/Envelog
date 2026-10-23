package cn.osmrpighead.envelog.util.date

import cn.osmrpighead.envelog.common.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

typealias DatePeriod = Pair<LocalDate, LocalDate>

val DatePeriod.length: Long get() = first countDaysTo second

operator fun DatePeriod.contains(date: LocalDate): Boolean = date in first..<second
operator fun DatePeriod.iterator(): Iterator<LocalDate> = object : Iterator<LocalDate> {
    private var cur = first
    override fun hasNext(): Boolean = cur < second
    override fun next(): LocalDate {
        if (!hasNext()) throw NoSuchElementException()
        return cur.also { cur = cur.plusDays(1) }
    }
}

infix fun DatePeriod.fullyBefore(other: DatePeriod): Boolean = second <= other.first
infix fun DatePeriod.hasBefore(other: DatePeriod): Boolean = first < other.first
infix fun DatePeriod.fullyAfter(other: DatePeriod): Boolean = first >= other.second
infix fun DatePeriod.hasAfter(other: DatePeriod): Boolean = second > other.second
infix fun DatePeriod.overlap(other: DatePeriod): Boolean = other.first < second && first < other.second
infix fun DatePeriod.intersect(other: DatePeriod): DatePeriod = maxOf(other.first, first) to minOf(other.second, second)

fun DatePeriod.format(style: FormatStyle = FormatStyle.SHORT, locale: Locale = AppLocale.current()): String =
    "${first.format(style, locale)} ~ ${second.format(style, locale)}"

fun DatePeriod.isMajorityOf(other: DatePeriod, fallback: DatePeriod.(DatePeriod) -> Boolean): Boolean {
    val x = (2 * intersect(other).length).compareTo(other.length)
    return when {
        x > 0 -> true
        x < 0 -> false
        else -> fallback(other)
    }
}

fun DatePeriod.splitIntoWeeks(firstDayOfWeek: DayOfWeek): List<DatePeriod> = buildList {
    var start = first
    var end = first.nextOrSame(firstDayOfWeek)
    while (end < second) {
        add(start to end)
        start = end
        end = end.plusDays(7)
    }
    add(start to second)
}
