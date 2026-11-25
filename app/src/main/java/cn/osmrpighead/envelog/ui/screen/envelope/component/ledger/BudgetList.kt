package cn.osmrpighead.envelog.ui.screen.envelope.component.ledger

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.yearMonth
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.component.AutoSizeText
import cn.osmrpighead.envelog.ui.icon.Icons
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.ui.util.FormatMMM
import cn.osmrpighead.envelog.ui.util.withColor
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun BudgetList(
    budgets: Collection<Budget>,
    yearMonth: YearMonth,
    onBudgetClick: (BudgetId) -> Unit,
    createBudget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleBudgets = budgets
        .filter { it.dateSpan overlaps yearMonth.toHalfOpenDateSpan() }
        .sortedBy { it.dateSpan.startDate }
    if (visibleBudgets.isEmpty()) Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.envelope_edit_budget_list_empty),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = createBudget,
            modifier = Modifier.padding(end = dimensionResource(R.dimen.space2))
        ) {
            Icon(
                imageVector = Icons.Add,
                contentDescription = null,
            )
            Text(text = stringResource(R.string.envelope_edit_budget_list_empty_action))
        }
    }
    else Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.space12))
            .fillMaxWidth(),
    ) {
        BudgetListHeader(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.space16))
                .padding(bottom = dimensionResource(R.dimen.space2)),
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.space12))
        ) {
            items(visibleBudgets, { it.id.toString() }) { budget ->
                BudgetRow(
                    budget = budget,
                    yearMonth = yearMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBudgetClick(budget.id) },
                )
            }
        }
    }
}

@Composable
private fun BudgetListHeader(modifier: Modifier = Modifier) {
    ProvideTextStyle(MaterialTheme.typography.labelSmall.withColor(EnvelogTheme.colorScheme.onSurfaceTertiary)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier,
        ) {
            Text(text = stringResource(R.string.envelope_edit_budget_list_date_range))
            Text(text = stringResource(R.string.envelope_edit_budget_list_amount))
        }
    }
}

@Composable
private fun BudgetRow(
    budget: Budget,
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.padding(
            horizontal = dimensionResource(R.dimen.space16),
            vertical = dimensionResource(R.dimen.space6),
        ),
    ) {
        DateSpan(
            span = budget.dateSpan.toClosed(),
            yearMonth = yearMonth,
        )
        AutoSizeText(
            text = budget.amount.formatAsAmount(Formatter.AmountInteger),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun DateSpan(
    span: ClosedDateSpan,
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    ProvideTextStyle(MaterialTheme.typography.titleLarge) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space2)),
            modifier = modifier,
        ) {
            Date(span.startDate, yearMonth)
            if (span.endDate != span.startDate) {
                Text(text = stringResource(R.string.symbol_en_dash))
                Date(span.endDate, yearMonth)
            }
        }
    }
}

@Composable
private fun Date(
    date: LocalDate,
    yearMonth: YearMonth,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space2)),
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        Text(text = date.dayOfMonth.toString())
        if (date.yearMonth != yearMonth) Text(
            text = FormatMMM.format(date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
