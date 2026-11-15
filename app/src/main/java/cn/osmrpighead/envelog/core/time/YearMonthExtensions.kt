package cn.osmrpighead.envelog.core.time

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

val YearMonth.first: LocalDate
    get() = atDay(1)
val YearMonth.next: YearMonth
    get() = plusMonths(1)

infix fun YearMonth.countMonthsTo(other: YearMonth): Long = ChronoUnit.MONTHS.between(this, other)
operator fun YearMonth.contains(date: LocalDate): Boolean = date.yearMonth == this
fun YearMonth.toHalfOpenDateSpan(): HalfOpenDateSpan = first untilExclusive next.first
