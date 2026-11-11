package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.time.YearMonth

@Composable
internal fun QuickSetSheetContent(
    formState: QuickSetFormState,
    controller: QuickSetSheetController,
    error: QuickSetError?,
    complete: Boolean,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            NumKeypadHost(
                controller.numKeypadHost,
                modifier = Modifier.disableVerticalDrag(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState()),
        ) {
            LabeledBox(
                label = stringResource(R.string.envelope_edit_quick_set_title),
                modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
            ) {
                QuickSetModeSection(
                    formState = formState,
                    controller = controller,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
                )
                QuickSetTargetRangeField(
                    yearMonth = formState.yearMonth,
                    span = formState.targetSpan,
                    enabled = formState.mode != QuickSetMode.MonthlyTotal,
                    onSpanChange = formState::updateTargetSpan,
                    onOpen = controller::onPickerOpen,
                )
                QuickSetFeedback(
                    preview = formState.preview,
                    validation = formState.validation,
                    error = error,
                )
                QuickSetActions(
                    complete = complete,
                    canApply = !complete && formState.canApply,
                    onCancel = onCancel,
                    onApply = onApply,
                )
            }
        }
    }
}

@Composable
private fun QuickSetTargetRangeField(
    yearMonth: YearMonth,
    span: ClosedDateSpan,
    enabled: Boolean,
    onSpanChange: (ClosedDateSpan) -> Unit,
    onOpen: () -> Unit,
) {
    val selectableDates = remember(yearMonth) {
        val monthSpan = yearMonth.toHalfOpenDateSpan()
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toLocalDate() in monthSpan

            override fun isSelectableYear(year: Int): Boolean = year == yearMonth.year
        }
    }

    DateRangePickerField(
        span = span,
        dialogBuilder = DateRangePickerDialogBuilder.Default(
            confirmEventHandler = DateRangePickerConfirmEventHandler.Default(onSpanChange),
            initialSpan = span,
            selectableDates = selectableDates,
        ),
        enabled = enabled,
        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_range_label)) },
        onOpen = onOpen,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacingMedium)),
    )
}

@Composable
private fun QuickSetFeedback(
    preview: QuickSetPreview?,
    validation: QuickSetValidation?,
    error: QuickSetError?,
) {
    if (preview != null) Text(
        text = preview.asText(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
    )
    if (validation != null) Text(
        text = validation.asText(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
    )
    if (error != null) Text(
        text = error.asText(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
    )
}

@Composable
private fun QuickSetActions(
    complete: Boolean,
    canApply: Boolean,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacingMedium)),
        horizontalArrangement = Arrangement.End,
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !complete,
        ) {
            Text(text = stringResource(R.string.common_action_cancel))
        }
        Button(
            onClick = onApply,
            enabled = canApply,
        ) {
            Text(text = stringResource(R.string.common_action_ok))
        }
    }
}

@Composable
internal fun QuickSetConflictDialog(
    conflict: QuickSetBudgetPlan,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val conflictTotal = conflict.conflicts.sumOf { it.amount }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_title)) },
        text = {
            Text(
                text = stringResource(
                    R.string.envelope_edit_quick_set_conflict_message,
                    conflict.conflicts.size,
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

@Composable
private fun QuickSetValidation.asText(): String = when (this) {
    QuickSetValidation.IntervalInvalid -> stringResource(R.string.envelope_edit_quick_set_interval_invalid)
    QuickSetValidation.AmountNonPositive -> stringResource(R.string.common_msg_amount_positive)
    QuickSetValidation.CannotGenerate -> stringResource(R.string.envelope_edit_quick_set_cannot_generate)
}

@Composable
private fun QuickSetError.asText(): String = when (this) {
    QuickSetError.EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
    QuickSetError.BudgetOverlaps -> stringResource(R.string.envelope_edit_msg_budget_overlaps)
}

@Composable
private fun Long.asPreviewAmount(): String {
    val prefix = stringResource(R.string.currency_cny_prefix)
    val suffix = stringResource(R.string.currency_cny_suffix)
    return prefix + formatAsAmount(Formatter.AmountConcise) + suffix
}
