package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.locale.AppLocale
import cn.osmrpighead.envelog.core.number.formatWithSign
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.countMonthsTo
import cn.osmrpighead.envelog.core.time.yearMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun relativeDate(start: LocalDate, end: LocalDate, nextWeek: LocalDate): String {
    val days = start countDaysTo end
    return if (days == 1L) stringResource(R.string.time_today)
    else if (end == nextWeek) stringResource(R.string.time_this_week)
    else stringResource(R.string.time_format_within_days, days)
}

@Composable
fun yearMonthText(yearMonth: YearMonth): String = stringResource(
    R.string.time_format_year_month,
    yearMonth.year,
    yearMonth.monthValue,
)

@Composable
fun dayRangeText(span: ClosedDateSpan, diffStyle: TextStyle): AnnotatedString =
    if (span.startDate != span.endDate) buildAnnotatedString {
        append(stringResource(R.string.time_format_day, span.startDate.dayOfMonth))
        append(stringResource(R.string.symbol_en_dash))
        append(stringResource(R.string.time_format_day, span.endDate.dayOfMonth))
        if (span.endDate.yearMonth != span.startDate.yearMonth) {
            withStyle(diffStyle.toSpanStyle()) {
                append(DateTimeFormatter.ofPattern("MMM", AppLocale.current()).format(span.endDate))
            }
        }
    } else AnnotatedString(stringResource(R.string.time_format_day, span.startDate.dayOfMonth))

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
