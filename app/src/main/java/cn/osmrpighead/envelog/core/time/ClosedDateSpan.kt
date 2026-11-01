package cn.osmrpighead.envelog.core.time

import cn.osmrpighead.envelog.core.locale.AppLocale
import java.time.LocalDate
import java.time.format.FormatStyle
import java.util.Locale

data class ClosedDateSpan(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    fun toHalfOpen() = HalfOpenDateSpan(startDate, endDate.plusDays(1))
    fun format(style: FormatStyle = FormatStyle.SHORT, locale: Locale = AppLocale.current()): String =
        "${startDate.format(style, locale)} ~ ${endDate.format(style, locale)}"
}
