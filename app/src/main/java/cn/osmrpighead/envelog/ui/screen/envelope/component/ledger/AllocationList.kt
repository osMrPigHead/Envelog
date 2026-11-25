package cn.osmrpighead.envelog.ui.screen.envelope.component.ledger

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.format
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.ui.component.AutoSizeText
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@Composable
private fun AllocationRow(
    allocation: AllocationWithInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        val blank = allocation.note.isBlank()
        Column {
            AutoSizeText(
                text = if (!blank) allocation.note
                else allocation.expense.happenedDate.format(),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (!blank) allocation.expense.happenedAtLocal.format()
                else allocation.expense.happenedTimeLocal.format(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AutoSizeText(
            text = allocation.amount.formatAsAmount(Formatter.AmountConcise),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun AllocationList(
    allocations: List<AllocationWithInfo>,
    modifier: Modifier = Modifier,
) {
    if (allocations.isEmpty()) Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(R.string.envelope_edit_allocation_list_empty),
            style = MaterialTheme.typography.labelLarge,
            color = EnvelogTheme.colorScheme.onSurfaceTertiary,
        )
    }
    else LazyColumn(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space16)),
        contentPadding = PaddingValues(
            top = dimensionResource(R.dimen.space4),
            bottom = dimensionResource(R.dimen.space16),
        ),
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.space24))
            .fillMaxWidth(),
    ) {
        items(allocations.sortedBy { it.expense.happenedAtLocal }, { it.expense.id.toString() }) {
            AllocationRow(it)
        }
    }
}
