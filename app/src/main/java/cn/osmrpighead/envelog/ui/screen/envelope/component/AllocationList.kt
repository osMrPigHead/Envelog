package cn.osmrpighead.envelog.ui.screen.envelope.component

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
import cn.osmrpighead.envelog.core.time.format
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.ui.component.statitem.StatItem
import cn.osmrpighead.envelog.ui.component.statitem.StatItemContent
import cn.osmrpighead.envelog.ui.component.statitem.StatItemSlot
import cn.osmrpighead.envelog.ui.component.statitem.StatItemStyle
import cn.osmrpighead.envelog.ui.component.statitem.asSlot
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount

@Composable
private fun AllocationRow(
    allocation: AllocationWithInfo,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatItem(
            style = StatItemStyle.MediumCenterStart,
            content = StatItemContent(
                title = StatItemSlot.Empty,
                value = StatItemSlot.Text("XXX"),
                caption = allocation.expense.happenedAt
                    .format(allocation.expense.happenedZone)
                    .asSlot(),
            ),
            modifier = Modifier.weight(1f),
        )
        StatItem(
            style = StatItemStyle.MediumCenterEnd,
            content = StatItemContent(
                title = StatItemSlot.Empty,
                value = allocation.amount.formatAsAmount(Formatter.AmountConcise).asSlot(),
                caption = StatItemSlot.Empty,
            ),
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingMedium)),
        )
    }
}

@Composable
fun AllocationList(
    allocations: List<AllocationWithInfo>,
    modifier: Modifier = Modifier,
) {
    if (allocations.isEmpty()) return
    Column(
        modifier = modifier
            .padding(horizontal = dimensionResource(R.dimen.spacingExtra))
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
    ) {
        for (allocation in allocations)
            AllocationRow(allocation)
    }
}
