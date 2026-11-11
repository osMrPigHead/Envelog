package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.countDaysTo
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun relativeDate(start: LocalDate, end: LocalDate, nextWeek: LocalDate): String {
    val days = start countDaysTo end
    return if (days == 1L) stringResource(R.string.time_today)
    else if (end == nextWeek) stringResource(R.string.time_this_week)
    else stringResource(R.string.time_format_within_days, days) // TODO: 本周
}

@Composable
fun yearMonthText(yearMonth: YearMonth): String = stringResource(
    R.string.time_format_year_month,
    yearMonth.year,
    yearMonth.monthValue,
)

@Composable
fun dayRangeText(start: LocalDate, endInclusive: LocalDate): String =
    if (start != endInclusive)
        stringResource(R.string.time_format_day, start.dayOfMonth) +
                stringResource(R.string.symbol_en_dash) +
                stringResource(R.string.time_format_day, endInclusive.dayOfMonth)
    else stringResource(R.string.time_format_day, start.dayOfMonth)

@Composable
fun dayOfWeekText(dayOfWeek: DayOfWeek): String = stringResource(dayOfWeek.shortNameRes())

@Composable
fun dayOfWeekRangeText(start: DayOfWeek, end: DayOfWeek): String = stringResource(
    R.string.time_format_day_of_week_range,
    dayOfWeekText(start),
    dayOfWeekText(end),
)

private fun DayOfWeek.shortNameRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.time_day_of_week_monday
    DayOfWeek.TUESDAY -> R.string.time_day_of_week_tuesday
    DayOfWeek.WEDNESDAY -> R.string.time_day_of_week_wednesday
    DayOfWeek.THURSDAY -> R.string.time_day_of_week_thursday
    DayOfWeek.FRIDAY -> R.string.time_day_of_week_friday
    DayOfWeek.SATURDAY -> R.string.time_day_of_week_saturday
    DayOfWeek.SUNDAY -> R.string.time_day_of_week_sunday
}
