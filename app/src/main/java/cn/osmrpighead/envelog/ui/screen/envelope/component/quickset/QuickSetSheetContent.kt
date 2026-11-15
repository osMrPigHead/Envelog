package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetPreview
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.time.YearMonth

@Composable
internal fun ColumnScope.QuickSetSheetContent(
    yearMonth: YearMonth,
    modalState: QuickSetFormModalState,
    numKeypadHost: NumKeypadHostState,
    update: (QuickSetField) -> Unit,
    dismiss: () -> Unit,
    apply: () -> Unit,
) {
    val config = modalState.config

    Column(
        modifier = Modifier
            .weight(1f)
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState()),
    ) {
        LabeledBox(
            label = stringResource(R.string.envelope_edit_quick_set_title),
            modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
        ) {
            QuickSetModeSection(
                enabled = modalState.state is FormState.Active && !modalState.dismissed,
                config = config,
                fieldErrors = modalState.fieldErrors,
                numKeypadHost = numKeypadHost,
                maxIntervalDays = config.targetSpan.length,
                update = update,
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
            )
            QuickSetTargetRangeField(
                yearMonth = yearMonth,
                span = config.targetSpan,
                enabled = config.mode != QuickSetMode.Monthly,
                onSpanChange = { update(QuickSetField.TargetSpan(it)) },
            )
            QuickSetFeedback(
                preview = modalState.preview,
                error = (modalState.state as? FormState.Active)?.error,
            )
            QuickSetActions(
                active = modalState.state is FormState.Active,
                isError = modalState.isError,
                dismiss = dismiss,
                apply = apply,
            )
        }
    }

    NumKeypadHost(
        numKeypadHost,
        modifier = Modifier.disableVerticalDrag(),
    )
}

@Composable
private fun QuickSetFeedback(
    preview: QuickSetPreview?,
    error: QuickSetFormError?,
) {
    if (preview != null) Text(
        text = preview.asText(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
    )
    val feedback = error?.asFeedbackText()
    if (feedback != null) Text(
        text = feedback,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
    )
}

@Composable
private fun QuickSetActions(
    active: Boolean,
    isError: Boolean,
    dismiss: () -> Unit,
    apply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacingMedium)),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = dismiss,
            enabled = active,
        ) {
            Text(text = stringResource(R.string.common_action_cancel))
        }
        Button(
            onClick = apply,
            enabled = active && !isError,
        ) {
            Text(text = stringResource(R.string.common_action_ok))
        }
    }
}

@Composable
internal fun QuickSetConflictDialog(
    conflicts: List<Budget>,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val conflictTotal = conflicts.sumOf { it.amount }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.envelope_edit_quick_set_conflict_message,
                    conflicts.size,
                    conflictTotal.asPreviewAmount(),
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.common_action_cancel))
            }
        },
    )
}

@Composable
private fun QuickSetPreview.asText(): String = when (this) {
    is QuickSetPreview.Daily -> stringResource(
        R.string.envelope_edit_quick_set_preview_daily,
        intervalDays,
        amount.asPreviewAmount(),
        days,
        totalAmount.asPreviewAmount(),
    )

    is QuickSetPreview.Weekly -> stringResource(
        R.string.envelope_edit_quick_set_preview_weekly,
        dayOfWeekRangeText(startDayOfWeek, endDayOfWeek),
        amount.asPreviewAmount(),
        days,
        totalAmount.asPreviewAmount(),
    )

    is QuickSetPreview.MonthlyTotal -> stringResource(
        R.string.envelope_edit_quick_set_preview_monthly,
        totalAmount.asPreviewAmount(),
        days,
    )
}

/** 区间非法与金额非正由各字段的 supportingText 呈现，重叠由确认框呈现，故此处返回 null。 */
@Composable
private fun QuickSetFormError.asFeedbackText(): String? = when (this) {
    QuickSetFormError.EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
    QuickSetFormError.CannotGenerate -> stringResource(R.string.envelope_edit_quick_set_cannot_generate)
}

@Composable
private fun Long.asPreviewAmount(): String {
    val prefix = stringResource(R.string.currency_cny_prefix)
    val suffix = stringResource(R.string.currency_cny_suffix)
    return prefix + formatAsAmount(Formatter.AmountConcise) + suffix
}
