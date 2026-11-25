package cn.osmrpighead.envelog.ui.screen.envelope.component.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.times
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetDisplay
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetDisplayType.*
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarWeekModel
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.Alpha
import cn.osmrpighead.envelog.ui.theme.CapsuleEnd
import cn.osmrpighead.envelog.ui.theme.CapsuleStart
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.ui.util.withAlpha
import java.time.LocalDate

@Composable
fun EnvelopeCalendar(
    model: EnvelopeCalendarModel,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space8)),
        modifier = modifier,
    ) {
        for (week in model.weeks) WeekBox(
            model = week,
            allocations = model.allocations,
            isBudgeted = model.isBudgeted,
            onClick = onClick,
        )
    }
}

@Composable
private fun WeekBox(
    model: EnvelopeCalendarWeekModel,
    allocations: Map<LocalDate, DailyAllocationSummary>,
    isBudgeted: Boolean,
    onClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (before, after, span, budgets) = model
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier,
    ) {
        if (isBudgeted) BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val day = maxWidth / 7
            for (budget in budgets) BudgetBar(
                model = budget,
                modifier = Modifier
                    .offset(x = budget.offset * day)
                    .width(budget.length * day),
            )
        }
        Row(
            modifier = Modifier
                .heightIn(dimensionResource(R.dimen.minTouchTarget))
                .height(IntrinsicSize.Min),
        ) {
            if (before > 0) Spacer(modifier = Modifier.weight(before))
            for (date in span) DayCell(
                date = date,
                allocation = allocations[date]?.amount,
                onClick = if (isBudgeted) {
                    { onClick(date) }
                } else null,
                modifier = Modifier.weight(1f),
            )
            if (after > 0) Spacer(modifier = Modifier.weight(after))
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    allocation: Long?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .clip(MaterialTheme.shapes.medium)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier,
            ),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = EnvelogTheme.typography.labelExtra,
        )
        allocation?.takeIf { it != 0L }?.formatAsAmount(Formatter.AmountConcise)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = EnvelogTheme.colorScheme.onSurfaceTertiary,
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.envelope_calendar_budget_track_space)))
    }
}

@Composable
private fun BudgetBar(
    model: BudgetDisplay,
    modifier: Modifier = Modifier,
) {
    val (_, length, type) = model
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.space4), vertical = dimensionResource(R.dimen.space4))
            .height(dimensionResource(R.dimen.envelope_calendar_budget_dot_size)),
    ) {
        Box(
            modifier = Modifier
                .clip(
                    when (type) {
                        Internal -> CircleShape
                        External -> RectangleShape
                        LeftBound -> CapsuleStart
                        RightBound -> CapsuleEnd
                    },
                )
                .background(MaterialTheme.colorScheme.inversePrimary.withAlpha(Alpha.High))
                .then(
                    if (type == Internal && length == 1f) Modifier
                        .size(dimensionResource(R.dimen.envelope_calendar_budget_dot_size))
                    else Modifier
                        .height(dimensionResource(R.dimen.envelope_calendar_budget_bar_height))
                        .fillMaxWidth(),
                ),
        )
    }
}
