package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.AutoSizeText
import cn.osmrpighead.envelog.ui.screen.overview.model.*
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsPercent
import cn.osmrpighead.envelog.ui.theme.AppTheme

@Composable
fun Summary(
    headline: HeadlineSummary,
    vice: List<SummaryItem>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space8)),
        modifier = modifier,
    ) {
        headline.Composable(Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            vice.forEachIndexed { index, item ->
                if (index != 0) Spacer(Modifier.width(dimensionResource(R.dimen.space16)))
                item.Composable(modifier = Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
fun HeadlineSummary.Composable(modifier: Modifier = Modifier) = when (this) {
    is Remaining -> Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.overview_summary_remaining_title),
            color = AppTheme.colorScheme.onSurfaceTertiary,
            style = MaterialTheme.typography.labelSmall,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space16)),
        ) {
            Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                AutoSizeText(
                    text = cents.formatAsAmount(Formatter.AmountConcise),
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space4)))
                AutoSizeText(
                    text = stringResource(
                        R.string.overview_summary_remaining_total,
                        total.formatAsAmount(Formatter.AmountConcise),
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            AutoSizeText(
                text = stringResource(
                    R.string.overview_summary_remaining_percent,
                    usedProportion.formatAsPercent(
                        Formatter.PercentInteger,
                    ) ?: stringResource(R.string.symbol_percent_nan),
                ),
                color = AppTheme.colorScheme.onSurfaceTertiary,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        LinearProgressIndicator(
            progress = { usedProportion.ratio.toFloat() },
            gapSize = dimensionResource(R.dimen.space0),
            drawStopIndicator = {},
            color = AppTheme.colorScheme.health, // TODO: Color
            modifier = Modifier.fillMaxWidth().padding(vertical = dimensionResource(R.dimen.space4)),
        )
    }
}

@Composable
private fun SimpleSummary(@StringRes title: Int, cents: Long, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.Bottom, modifier = modifier) {
        Text(
            text = stringResource(title),
            color = AppTheme.colorScheme.onSurfaceTertiary,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = dimensionResource(R.dimen.space8)),
        )
        AutoSizeText(
            text = cents.formatAsAmount(Formatter.AmountConcise),
            style = MaterialTheme.typography.titleLarge,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun SummaryItem.Composable(modifier: Modifier = Modifier) = when (this) {
    is Balance -> SimpleSummary(R.string.overview_summary_balance_title, cents, modifier)
    is Expenses -> SimpleSummary(R.string.overview_summary_expense_title, cents, modifier)
    is Remaining -> SimpleSummary(R.string.overview_summary_remaining_title, cents, modifier)
}
