package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.statitem.*
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.CapsuleEnd
import cn.osmrpighead.envelog.ui.theme.CapsuleStart
import cn.osmrpighead.envelog.util.date.*
import java.time.DayOfWeek
import java.time.LocalDate

private sealed class Budget {
    abstract val period: DatePeriod
    abstract val amount: Long

    data class Internal(override val period: DatePeriod, override val amount: Long) : Budget()
    data class External(override val period: DatePeriod, override val amount: Long) : Budget()
    data class LeftBound(override val period: DatePeriod, override val amount: Long) : Budget()
    data class RightBound(override val period: DatePeriod, override val amount: Long) : Budget()
}

@Composable
private fun DayCell(
    style: StatItemStyle,
    date: LocalDate,
    allocation: Long?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    StatItem(
        style = style,
        content = StatItemContent(
            title = StatItemSlot.Empty,
            value = date.dayOfMonth.toString().asSlot(),
            caption = allocation?.formatAsAmount(Formatter.AmountConcise)?.asSlot() ?: StatItemSlot.Space,
        ),
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun BudgetCapsule(budget: Budget, fill: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = budget.amount.formatAsAmount(Formatter.AmountConcise),
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = when (budget) {
                        is Budget.Internal -> CircleShape
                        is Budget.External -> RectangleShape
                        is Budget.LeftBound -> CapsuleStart
                        is Budget.RightBound -> CapsuleEnd
                    },
                )
                .padding(horizontal = dimensionResource(R.dimen.spacingSmall))
                .then(if (fill) Modifier.fillMaxWidth() else Modifier),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun RowScope.DaySpacer(period: DatePeriod, modifier: Modifier = Modifier) {
    if (period.length > 0) Spacer(modifier = modifier.weight(period.length.toFloat()))
}

@Composable
fun BudgetCalendar(
    period: DatePeriod,
    model: BudgetCalendarModel,
    firstDayOfWeek: DayOfWeek,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val weeks = period.splitIntoWeeks(firstDayOfWeek).map { Pair(it, emptyList<Budget>().toMutableList()) }
    for ((_, _, startDate, endDate, amount) in model.budgets) {
        for ((week, budgets) in weeks) {
            val full = startDate untilExclusive endDate
            val intersect = full intersect week
            if (intersect.length <= 0) continue
            budgets += when (Pair(full hasBefore intersect, full hasAfter intersect)) {
                true to true -> Budget.External(intersect, amount)
                false to true -> Budget.LeftBound(intersect, amount)
                true to false -> Budget.RightBound(intersect, amount)
                else -> Budget.Internal(intersect, amount)
            }
        }
    }
    val style = StatItemStyle.SmallCenter
    Column(modifier = modifier) {
        for ((week, budgets) in weeks) {
            val firstDayOfThisWeek = week.start.previousOrSame(firstDayOfWeek)
            val firstDayOfNextWeek = week.endExclusive.nextOrSame(firstDayOfWeek)
            Row {
                DaySpacer(firstDayOfThisWeek untilExclusive week.start)
                for (date in week) DayCell(style, date, model.allocations[date], { onClick(date) }, Modifier.weight(1f))
                DaySpacer(week.endExclusive untilExclusive firstDayOfNextWeek)
            }
            Row {
                budgets.sortBy { it.period.start }
                var date = firstDayOfThisWeek
                for (budget in budgets) {
                    DaySpacer(date untilExclusive budget.period.start)
                    BudgetCapsule(
                        budget = budget,
                        fill = budget.period.length != 1L,
                        modifier = Modifier
                            .clickable { onClick(budget.period.start) }
                            .padding(horizontal = dimensionResource(R.dimen.spacingMedium))
                            .weight(budget.period.length.toFloat()),
                    )
                    date = budget.period.endExclusive
                }
                DaySpacer(date untilExclusive firstDayOfNextWeek)
            }
        }
    }
}
