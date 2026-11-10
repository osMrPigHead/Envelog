package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.countDaysTo
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
