package cn.osmrpighead.envelog.util.date

import java.time.LocalDate
import java.time.YearMonth

val YearMonth.first: LocalDate
    get() = atDay(1)
val YearMonth.next: YearMonth
    get() = plusMonths(1)

operator fun YearMonth.contains(date: LocalDate): Boolean = date.yearMonth == this
