package cn.osmrpighead.envelog.core.time

import cn.osmrpighead.envelog.core.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.FormatStyle
import java.util.Locale

data class ClosedDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val length = toHalfOpen().length

    fun toHalfOpen() = HalfOpenDateSpan(startDate, endDate.plusDays(1))
    fun format(style: FormatStyle = FormatStyle.SHORT, locale: Locale = AppLocale.current()): String =
        if (length == 1L) startDate.format(style, locale)
        else "${startDate.format(style, locale)} ~ ${endDate.format(style, locale)}"
}
