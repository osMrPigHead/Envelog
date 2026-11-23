package cn.osmrpighead.envelog.ui.screen.overview.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.next
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.component.AutoSizeText
import cn.osmrpighead.envelog.ui.icon.Icons
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsPercent
import cn.osmrpighead.envelog.ui.text.Formatter.formattedAmount
import cn.osmrpighead.envelog.ui.text.orDefault
import cn.osmrpighead.envelog.ui.text.prefix
import cn.osmrpighead.envelog.ui.text.relativeDate
import cn.osmrpighead.envelog.ui.text.suffix
import cn.osmrpighead.envelog.ui.text.withCurrency
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
private fun EnvelopeCardShell(
    model: EnvelopeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rightSlot: @Composable BoxScope.() -> Unit,
) {
    Card(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.space16)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space16)),
        ) {
            Column(modifier = Modifier.weight(3f)) {
                Row {
                    Text(
                        text = model.name,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .alignByBaseline(),
                    )
                    if (model is EnvelopeSummary.Budgeted) {
                        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space8)))
                        Text(
                            text = stringResource(
                                R.string.overview_envelope_card_used_percent,
                                model.usedProportion
                                    .formatAsPercent(Formatter.PercentInteger)
                                    .orDefault(R.string.symbol_percent_nan),
                            ),
                            overflow = TextOverflow.MiddleEllipsis,
                            maxLines = 1,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                AutoSizeText(
                    text =
                        if (model is EnvelopeSummary.Budgeted) stringResource(
                            R.string.common_format_fraction,
                            model.allocated.formatAsAmount(Formatter.AmountConcise),
                            model.totalBudget.formatAsAmount(Formatter.AmountConcise),
                        ).withCurrency(model.currency)
                        else stringResource(
                            R.string.overview_envelope_card_non_budget_caption,
                            model.allocated.formatAsAmount(Formatter.AmountConcise).withCurrency(model.currency),
                        ),
                    overflow = TextOverflow.MiddleEllipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Box(modifier = Modifier.weight(2f)) {
                rightSlot()
            }
        }
    }
}

@Composable
private fun EnvelopeSummary.Budgeted.relativeDate(today: LocalDate): String =
    if (currentSpan?.endDate == null) stringResource(R.string.time_today)
    else relativeDate(today, currentSpan.endDate, today.next(DayOfWeek.MONDAY))

@Composable
private fun EnvelopeCardViewContent(
    model: EnvelopeSummary,
    today: LocalDate,
    diffColor: @Composable (diffAmount: Long) -> Color,
    modifier: Modifier = Modifier,
) = when (model) {
    is EnvelopeSummary.Budgeted -> {
        val formattedDiff = model.priorDifference.formattedAmount(Formatter.AmountSignedInteger)
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(
                    R.string.overview_envelope_card_available_title,
                    model.relativeDate(today),
                ),
                color = EnvelogTheme.colorScheme.onSurfaceTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
            Row {
                AutoSizeText(
                    text = model.currentAvailable.formatAsAmount(Formatter.AmountInteger),
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                )
                if (formattedDiff.toBigDecimal().compareTo(BigDecimal.ZERO) != 0) {
                    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space2)))
                    AutoSizeText(
                        text = formattedDiff.toString(),
                        color = diffColor(model.priorDifference),
                        style = MaterialTheme.typography.bodyMedium,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).alignByBaseline(),
                    )
                }
            }
        }
    }

    is EnvelopeSummary.NonBudget -> {
        Box(
            modifier = modifier.fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = stringResource(R.string.overview_envelope_card_no_budget),
                color = EnvelogTheme.colorScheme.onSurfaceTertiary,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun EnvelopeCardManageContent(
    canDelete: Boolean,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        if (canDelete) IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Delete,
                contentDescription = stringResource(R.string.overview_envelope_action_delete),
            )
        } else IconButton(onClick = onArchiveClick) {
            Icon(
                imageVector = Icons.Inventory2,
                contentDescription = stringResource(R.string.overview_envelope_action_archive),
            )
        }
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Edit,
                contentDescription = stringResource(R.string.overview_envelope_action_edit),
            )
        }
    }
}

@Composable
fun EnvelopeCard(
    model: EnvelopeSummary,
    today: LocalDate,
    managing: Boolean,
    diffColor: @Composable (diffAmount: Long) -> Color,
    onClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EnvelopeCardShell(
        model = model,
        onClick = onClick,
        modifier = modifier,
    ) {
        AnimatedContent(
            targetState = managing,
            contentAlignment = Alignment.CenterEnd,
            transitionSpec = {
                val direction = if (targetState) 1 else -1
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                    initialOffsetX = { width -> direction * width / 4 },
                ) + fadeIn(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                    targetOffsetX = { width -> -direction * width / 4 },
                ) + fadeOut(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                (enter togetherWith exit).using(null)
            },
        ) { isManaging ->
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.minTouchTarget)),
            ) {
                if (isManaging) EnvelopeCardManageContent(
                    canDelete = model.canDelete,
                    onArchiveClick = onArchiveClick,
                    onDeleteClick = onDeleteClick,
                    onEditClick = onEditClick,
                )
                else EnvelopeCardViewContent(
                    model = model,
                    today = today,
                    diffColor = diffColor,
                )
            }
        }
    }
}
