package cn.osmrpighead.envelog.ui.overview.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.statitem.StatItem
import cn.osmrpighead.envelog.ui.component.statitem.StatItemContent
import cn.osmrpighead.envelog.ui.component.statitem.StatItemSlot
import cn.osmrpighead.envelog.ui.component.statitem.StatItemStyle
import cn.osmrpighead.envelog.ui.overview.model.SummaryItem
import cn.osmrpighead.envelog.ui.text.orDefault
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsPercent
import java.time.YearMonth

@Composable
fun Summary(
    yearMonth: YearMonth,
    items: List<SummaryItem>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        verticalAlignment = Alignment.Bottom,
    ) {
        StatItem(
            style = StatItemStyle.ExtremeCenterStart,
            content = StatItemContent(
                title = StatItemSlot.Text(stringResource(R.string.year, yearMonth.year)),
                value = StatItemSlot.Text(stringResource(R.string.month, yearMonth.monthValue)),
                caption = StatItemSlot.Empty,
            ),
        )
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    top = dimensionResource(R.dimen.spacingThin),
                    bottom = dimensionResource(R.dimen.spacingMini),
                ),
        )
        val statItemStyle = StatItemStyle.LargeCenterStart
        val reserveCaption = items.any { it.caption }
        for (item in items)
            StatItem(
                style = statItemStyle,
                content = item.statItemContent(reserveCaption),
                modifier = Modifier.weight(1f),
            )
    }
}

private val Boolean.Empty: StatItemSlot
    get() = if (this) StatItemSlot.Space else StatItemSlot.Empty

@Composable
fun SummaryItem.statItemContent(reserveCaption: Boolean): StatItemContent = when (this) {
    is SummaryItem.Balance -> StatItemContent(
        title = StatItemSlot.Text(stringResource(R.string.summary_balance_title)),
        value = StatItemSlot.Text(cents.formatAsAmount(Formatter.AmountStandard)),
        caption = reserveCaption.Empty,
    )

    is SummaryItem.Expenses -> StatItemContent(
        title = StatItemSlot.Text(stringResource(R.string.summary_expenses_title)),
        value = StatItemSlot.Text(cents.formatAsAmount(Formatter.AmountStandard)),
        caption = reserveCaption.Empty,
    )

    is SummaryItem.Remaining -> StatItemContent(
        title = StatItemSlot.Text(stringResource(R.string.summary_remaining_title)),
        value = StatItemSlot.Text(cents.formatAsAmount(Formatter.AmountStandard)),
        caption = StatItemSlot.Text(
            stringResource(
                R.string.summary_remaining_caption,
                usedProportion.formatAsPercent(Formatter.PercentInteger).orDefault(R.string.text_percent_nan),
            ),
        ),
    )
}
