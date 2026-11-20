package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.component.statitem.*
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayRangeText
import java.time.YearMonth

@Composable
private fun BudgetRow(
    budget: Budget,
    dateRange: AnnotatedString,
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
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing4)),
        )
    }
}

@Composable
private fun BudgetListHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(
            style = StatItemStyle.SmallCenterStart,
            content = StatItemContent(
                title = stringResource(R.string.envelope_edit_budget_list_date_range).asSlot(),
                value = StatItemSlot.Empty,
                caption = StatItemSlot.Empty,
            ),
            modifier = Modifier.weight(1f),
        )
        StatItem(
            style = StatItemStyle.SmallCenterEnd,
            content = StatItemContent(
                title = stringResource(R.string.envelope_edit_budget_list_amount).asSlot(),
                value = StatItemSlot.Empty,
                caption = StatItemSlot.Empty,
            ),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing4)),
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
    val visibleBudgets = budgets
        .filter { it.dateSpan overlaps yearMonth.toHalfOpenDateSpan() }
        .sortedBy { it.dateSpan.startDate }
    if (visibleBudgets.isEmpty()) return
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.spacing10))
            .fillMaxWidth(),
    ) {
        BudgetListHeader(modifier = Modifier.padding(bottom = dimensionResource(R.dimen.spacing4)))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing6))) {
            items(visibleBudgets, { it.id.toString() }) {
                BudgetRow(
                    budget = it,
                    dateRange = dayRangeText(
                        span = it.dateSpan.toClosed(),
                        diffStyle = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    ),
                    onBudgetClick = onBudgetClick,
                )
            }
        }
    }
}
