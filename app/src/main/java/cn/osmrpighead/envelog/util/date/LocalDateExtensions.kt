package cn.osmrpighead.envelog.util.date

import cn.osmrpighead.envelog.common.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

infix fun LocalDate.countDaysTo(other: LocalDate): Long = other.toEpochDay() - toEpochDay()

val LocalDate.yearMonth: YearMonth get() = YearMonth.from(this)

fun LocalDate.next(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.next(dayOfWeek))
fun LocalDate.nextOrSame(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.nextOrSame(dayOfWeek))
fun LocalDate.previous(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.previous(dayOfWeek))
fun LocalDate.previousOrSame(dayOfWeek: DayOfWeek): LocalDate = with(TemporalAdjusters.previousOrSame(dayOfWeek))

fun LocalDate.format(style: FormatStyle = FormatStyle.SHORT, locale: Locale = AppLocale.current()): String =
    format(DateTimeFormatter.ofLocalizedDate(style).withLocale(locale))
