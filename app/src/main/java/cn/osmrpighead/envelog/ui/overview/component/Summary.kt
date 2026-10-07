package cn.osmrpighead.envelog.ui.overview.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.StatItem
import cn.osmrpighead.envelog.ui.component.StatItemStyle
import cn.osmrpighead.envelog.ui.component.rememberValueHeight
import cn.osmrpighead.envelog.ui.overview.SummaryItem
import cn.osmrpighead.envelog.ui.overview.SummaryType
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.annotation.LangPreviews
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
            style = StatItemStyle.of(StatItemStyle.Size.Extreme, Alignment.Start),
            title = stringResource(R.string.year, yearMonth.year),
            value = stringResource(R.string.month, yearMonth.monthValue),
        )
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    top = dimensionResource(R.dimen.spacingThin),
                    bottom = dimensionResource(R.dimen.spacingMini),
                ),
        )
        val statItemStyle = StatItemStyle.of(StatItemStyle.Size.Large, Alignment.Start)
        val statValueHeight = statItemStyle.rememberValueHeight()
        val hasCaption = items.any { it.captionValue != null }
        for (item in items)
            StatItem(
                style = statItemStyle,
                modifier = Modifier.weight(1f),
                valueHeight = statValueHeight,
                title = item.title,
                value = item.value,
                caption = if (hasCaption) item.caption ?: "" else item.caption,
            )
    }
}

private val SummaryItem.title: String
    @Composable get() {
        return when (type) {
            SummaryType.Remaining -> stringResource(R.string.summary_remaining_title)
            SummaryType.Expenses -> stringResource(R.string.summary_expenses_title)
            SummaryType.Balance -> stringResource(R.string.summary_balance_title)
        }
    }

private val SummaryItem.caption: String?
    @Composable get() {
        return when (type) {
            SummaryType.Remaining -> stringResource(R.string.summary_budgetRemaining_caption, captionValue!!)
            else -> null
        }
    }

@Preview
@LangPreviews
@Composable
private fun SummaryPreview() {
    EnvelogTheme {
        Summary(
            yearMonth = YearMonth.of(2026, 9),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            items = listOf(
                SummaryItem(
                    type = SummaryType.Remaining,
                    value = "11451411.14",
                    captionValue = "91",
                ),
                SummaryItem(
                    type = SummaryType.Expenses,
                    value = "9191.78",
                ),
                SummaryItem(
                    type = SummaryType.Balance,
                    value = "-1451.14",
                ),
            ),
        )
    }
}
