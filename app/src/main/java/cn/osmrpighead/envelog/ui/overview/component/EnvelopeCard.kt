package cn.osmrpighead.envelog.ui.overview.component

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.StatItem
import cn.osmrpighead.envelog.ui.component.StatItemStyle
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.overview.BudgetRelated
import cn.osmrpighead.envelog.ui.overview.BudgetScope
import cn.osmrpighead.envelog.ui.overview.CurrencyType
import cn.osmrpighead.envelog.ui.overview.EnvelopeModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.MotionTokens
import cn.osmrpighead.envelog.util.annotation.LangPreviews
import java.util.*

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
                style = StatItemStyle.of(StatItemStyle.Size.Large, Alignment.Start).run {
                    copy(valueStyle = valueStyle.copy(fontWeight = FontWeight.Normal))
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(start = dimensionResource(R.dimen.spacingBold))
                    .padding(vertical = dimensionResource(R.dimen.spacingBold)),
                value = buildAnnotatedString {
                    append(model.name)
                    append(" ")
                    if (model.budgetRelated != null)
                        withStyle(
                            MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            ).toSpanStyle(),
                        ) { append(stringResource(R.string.envelope_used, model.budgetRelated.usedPercent)) }
                },
                caption = if (model.budgetRelated != null) stringResource(
                    R.string.envelope_caption,
                    model.usedAmount, model.budgetRelated.totalAmount,
                    model.currency.prefix, model.currency.suffix,
                ) else stringResource(
                    R.string.envelope_caption_non_budget,
                    model.usedAmount,
                    model.currency.prefix, model.currency.suffix,
                ),
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

@Composable
private fun EnvelopeCardViewContent(
    model: EnvelopeModel,
    diffColor: Color,
    modifier: Modifier = Modifier,
) {
    if (model.budgetRelated != null)
        StatItem(
            style = StatItemStyle.of(StatItemStyle.Size.Medium, Alignment.End),
            modifier = modifier,
            title = stringResource(R.string.envelope_available, model.budgetRelated.availableFor.label),
            value = buildAnnotatedString {
                append(model.budgetRelated.availableNow)
                append("\u200a")
                withStyle(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = diffColor,
                        letterSpacing = 0.0.sp,
                    ).toSpanStyle(),
                ) { append(model.budgetRelated.availableDiff) }
            },
        )
}

private val BudgetScope.label: String
    @Composable get() = when (this) {
        is BudgetScope.Today -> stringResource(R.string.time_today)
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
    diffColor: Color,
    onDeleteClick: () -> Unit,
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

private val previewEnvelopeModel = EnvelopeModel(
    id = UUID.randomUUID(),
    name = "餐饮",
    usedAmount = "1415.14",
    currency = CurrencyType.CNY,
    diffNeg = false,
    budgetRelated = BudgetRelated(
        usedPercent = "74",
        totalAmount = "1919.81",
        availableFor = BudgetScope.Today,
        availableNow = "67",
        availableDiff = "+16",
    ),
)

@Preview
@LangPreviews
@Composable
private fun EnvelopeCardViewPreview() {
    EnvelogTheme {
        EnvelopeCardShell(
            model = previewEnvelopeModel,
            modifier = Modifier.padding(dimensionResource(R.dimen.spacingBold)),
        ) {
            EnvelopeCardViewContent(
                model = previewEnvelopeModel,
                diffColor = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingBold)),
            )
        }
    }
}

@Preview
@LangPreviews
@Composable
private fun EnvelopeCardManagePreview() {
    EnvelogTheme {
        EnvelopeCardShell(
            model = previewEnvelopeModel,
            modifier = Modifier.padding(dimensionResource(R.dimen.spacingBold)),
        ) {
            EnvelopeCardManageContent(
                onArchiveClick = {},
                onEditClick = {},
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingMedium)),
            )
        }
    }
}

@Preview
@Composable
private fun EnvelopeCardPreview() {
    EnvelogTheme {
        var managing by remember { mutableStateOf(false) }
        Column {
            Button(onClick = { managing = !managing }) {
                Text("Toggle Managing")
            }
            EnvelopeCard(
                model = previewEnvelopeModel,
                managing = managing,
                diffColor = MaterialTheme.colorScheme.error,
                onDeleteClick = {},
                onArchiveClick = {},
                onEditClick = {},
            )
        }
    }
}
