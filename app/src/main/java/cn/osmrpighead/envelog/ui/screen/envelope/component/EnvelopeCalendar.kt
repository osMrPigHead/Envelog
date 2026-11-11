package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextOverflow
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.component.statitem.*
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetDisplay
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp
import cn.osmrpighead.envelog.ui.theme.CapsuleEnd
import cn.osmrpighead.envelog.ui.theme.CapsuleStart
import java.time.LocalDate

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
            caption = allocation?.takeIf { it != 0L }?.formatAsAmount(Formatter.AmountConcise)?.asSlot()
                ?: StatItemSlot.Space,
        ),
        modifier = modifier.clickable(onClick = onClick),
    )
}

@Composable
private fun BudgetCapsule(budget: BudgetDisplay.NoSpace, fill: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = budget.amount.formatAsAmount(Formatter.AmountConcise),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = when (budget) {
                        is BudgetDisplay.Internal -> CircleShape
                        is BudgetDisplay.External -> RectangleShape
                        is BudgetDisplay.LeftBound -> CapsuleStart
                        is BudgetDisplay.RightBound -> CapsuleEnd
                    },
                )
                .padding(horizontal = dimensionResource(R.dimen.spacingSmall))
                .then(if (fill) Modifier.fillMaxWidth() else Modifier),
        )
    }
}

@Composable
fun BudgetCalendar(
    model: EnvelopeCalendarModel,
    onDateClick: (LocalDate) -> Unit,
    onBudgetClick: (BudgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = StatItemStyle.SmallCenter
    val spacerModifier = Modifier.height(MaterialTheme.typography.labelSmall.lineHeightAsDp)
    Column(modifier = modifier) {
        for ((before, after, week, budgets) in model.weeks) {
            Row(modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall))) {
                if (before > 0) Spacer(modifier = spacerModifier.weight(before.toFloat()))
                for (date in week) {
                    val budgetId = model.dates[date]?.budgetId
                    DayCell(
                        style,
                        date,
                        model.dates[date]?.allocationSummary?.amount,
                        if (budgetId == null) {
                            { onDateClick(date) }
                        } else {
                            { onBudgetClick(budgetId) }
                        },
                        Modifier.weight(1f),
                    )
                }
                if (after > 0) Spacer(modifier = spacerModifier.weight(after.toFloat()))
            }
            if (budgets.isNotEmpty()) Row {
                for (budget in budgets) when (budget) {
                    is BudgetDisplay.NoSpace -> BudgetCapsule(
                        budget = budget,
                        fill = budget.length != 1L,
                        modifier = Modifier
                            .clickable { onBudgetClick(budget.id) }
                            .padding(horizontal = dimensionResource(R.dimen.spacingMedium))
                            .weight(budget.length.toFloat()),
                    )

                    is BudgetDisplay.Spacer -> Spacer(modifier = spacerModifier.weight(budget.length.toFloat()))
                }
            }
        }
    }
}
