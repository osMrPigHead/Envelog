package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun relativeDate(start: LocalDate, end: LocalDate): String {
    val days = ChronoUnit.DAYS.between(start, end)
    return if (days == 1L) stringResource(R.string.time_today)
    else stringResource(R.string.time_within_days, days)
}
