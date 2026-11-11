package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.component.statitem.StatItem
import cn.osmrpighead.envelog.ui.component.statitem.StatItemContent
import cn.osmrpighead.envelog.ui.component.statitem.StatItemSlot
import cn.osmrpighead.envelog.ui.component.statitem.StatItemStyle
import cn.osmrpighead.envelog.ui.component.statitem.asSlot
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayRangeText
import java.time.YearMonth

@Composable
private fun BudgetRow(
    budget: Budget,
    dateRange: String,
    onBudgetClick: (BudgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onBudgetClick(budget.id) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(
            style = StatItemStyle.MediumCenterStart,
            content = StatItemContent(
                title = StatItemSlot.Empty,
                value = dateRange.asSlot(),
                caption = StatItemSlot.Empty,
            ),
            modifier = Modifier.weight(1f),
        )
        StatItem(
            style = StatItemStyle.MediumCenterEnd,
            content = StatItemContent(
                title = StatItemSlot.Empty,
                value = budget.amount.formatAsAmount(Formatter.AmountConcise).asSlot(),
                caption = StatItemSlot.Empty,
            ),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingMedium)),
        )
    }
}

@Composable
fun BudgetList(
    budgets: Collection<Budget>,
    yearMonth: YearMonth,
    onBudgetClick: (BudgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val month = yearMonth.toHalfOpenDateSpan()
    val visibleBudgets = budgets.mapNotNull { budget ->
        val span = budget.dateSpan intersect month
        if (span.startDate < span.endDate) budget to span else null
    }.sortedBy { (_, span) -> span.startDate }
    if (visibleBudgets.isEmpty()) return
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.spacingExtra))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
    ) {
        for ((budget, span) in visibleBudgets)
            BudgetRow(
                budget = budget,
                dateRange = dayRangeText(span.startDate, span.endDate.minusDays(1)),
                onBudgetClick = onBudgetClick,
            )
    }
}
