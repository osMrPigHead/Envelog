package cn.osmrpighead.envelog.ui.overview.component

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.CurrencyType
import cn.osmrpighead.envelog.ui.component.statitem.*
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.overview.model.EnvelopeModel
import cn.osmrpighead.envelog.ui.text.orDefault
import cn.osmrpighead.envelog.ui.text.relativeDate
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsPercent
import cn.osmrpighead.envelog.ui.text.Formatter.formattedAmount
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.math.BigDecimal

@Composable
private fun EnvelopeCardShell(
    model: EnvelopeModel,
    modifier: Modifier = Modifier,
    rightSlot: @Composable RowScope.() -> Unit,
) {
    Card(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(
                style = StatItemStyle.LargeCenterStart.update(
                    value = { copy(textStyle = textStyle.copy(fontWeight = FontWeight.Normal)) },
                ),
                content = StatItemContent(
                    title = StatItemSlot.Empty,
                    value = StatItemSlot.Text(
                        buildAnnotatedString {
                            append(model.name)
                            append(stringResource(R.string.separator_word))
                            if (model.budgetRelated != null)
                                withStyle(
                                    MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ).toSpanStyle(),
                                ) {
                                    append(
                                        stringResource(
                                            R.string.envelope_used,
                                            model.budgetRelated.usedProportion
                                                .formatAsPercent(Formatter.PercentInteger)
                                                .orDefault(R.string.text_percent_nan),
                                        ),
                                    )
                                }
                        },
                    ),
                    caption = StatItemSlot.Text(
                        if (model.budgetRelated != null) stringResource(
                            R.string.envelope_caption,
                            model.usedAmount.formatAsAmount(Formatter.AmountConcise),
                            model.budgetRelated.budgetAmount.formatAsAmount(Formatter.AmountConcise),
                            model.currency.prefix,
                            model.currency.suffix,
                        ) else stringResource(
                            R.string.envelope_caption_non_budget,
                            model.usedAmount.formatAsAmount(Formatter.AmountConcise),
                            model.currency.prefix, model.currency.suffix,
                        ),
                    ),
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(R.dimen.spacingBold))
                    .padding(vertical = dimensionResource(R.dimen.spacingBold)),
            )
            rightSlot()
        }
    }
}

private val CurrencyType.prefix: String
    @Composable get() = when (this) {
        CurrencyType.CNY -> stringResource(R.string.currency_cny_prefix)
    }

private val CurrencyType.suffix: String
    @Composable get() = when (this) {
        CurrencyType.CNY -> stringResource(R.string.currency_cny_suffix)
    }

private val EnvelopeModel.BudgetRelated.relativeDate: String
    @Composable get() = if (endDate == null) stringResource(R.string.time_today)
    else relativeDate(today, endDate)

@Composable
private fun EnvelopeCardViewContent(
    model: EnvelopeModel,
    diffColor: @Composable (diffAmount: Long) -> Color,
    modifier: Modifier = Modifier,
) {
    if (model.budgetRelated != null)
        StatItem(
            style = StatItemStyle.MediumCenterEnd,
            content = StatItemContent(
                title = StatItemSlot.Text(stringResource(R.string.envelope_available, model.budgetRelated.relativeDate)),
                value = StatItemSlot.Text(
                    buildAnnotatedString {
                        append(model.budgetRelated.currentAvailable.formatAsAmount(Formatter.AmountInteger))
                        val formattedDiff = model.budgetRelated.diffAmount.formattedAmount(Formatter.AmountSignedInteger)
                        if (formattedDiff.toBigDecimal().compareTo(BigDecimal.ZERO) != 0) {
                            append(stringResource(R.string.separator_thin))
                            withStyle(
                                MaterialTheme.typography.bodyMedium.copy(
                                    color = diffColor(model.budgetRelated.diffAmount),
                                    letterSpacing = 0.0.sp,
                                ).toSpanStyle(),
                            ) {
                                append(formattedDiff.toString())
                            }
                        }
                    },
                ),
                caption = StatItemSlot.Empty,
            ),
            modifier = modifier,
        )
}

@Composable
private fun EnvelopeCardManageContent(
    onArchiveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        IconButton(onClick = onArchiveClick) {
            Icon(
                imageVector = Icon.Inventory2,
                contentDescription = stringResource(R.string.envelope_manage_archive),
            )
        }
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icon.Edit,
                contentDescription = stringResource(R.string.envelope_manage_edit),
            )
        }
    }
}

@Composable
fun EnvelopeCard(
    model: EnvelopeModel,
    managing: Boolean,
    diffColor: @Composable (diffAmount: Long) -> Color,
    onArchiveClick: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EnvelopeCardShell(
        model = model,
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
            Box(contentAlignment = Alignment.CenterEnd) {
                if (isManaging) EnvelopeCardManageContent(
                    onArchiveClick = onArchiveClick,
                    onEditClick = onEditClick,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingMedium)),
                )
                else EnvelopeCardViewContent(
                    model = model,
                    diffColor = diffColor,
                    modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingBold)),
                )
            }
        }
    }
}
