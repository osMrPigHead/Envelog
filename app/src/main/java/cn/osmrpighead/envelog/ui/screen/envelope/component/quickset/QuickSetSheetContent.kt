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
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetSchedule
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.time.YearMonth

@Composable
internal fun QuickSetSheetContent(
    yearMonth: YearMonth,
    modalState: QuickSetFormModalState,
    numKeypadHost: NumKeypadHostState,
    onModeChange: () -> Unit,
    onTextInputFocus: () -> Unit,
    onPickerOpen: () -> Unit,
    onUpdate: (QuickSetFormState) -> Unit,
    onCancel: () -> Unit,
    onApply: () -> Unit,
) {
    val config = modalState.config
    val schedule = modalState.schedule
    val error = (modalState.state as? FormState.Active)?.error
    val isBusy = modalState.state is FormState.Busy
    val maxIntervalDays = config.targetSpan.maxIntervalDays()

    Scaffold(
        bottomBar = {
            NumKeypadHost(
                numKeypadHost,
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
                    config = config,
                    error = error,
                    numKeypadHost = numKeypadHost,
                    onModeChange = onModeChange,
                    onTextInputFocus = onTextInputFocus,
                    onPickerOpen = onPickerOpen,
                    maxIntervalDays = maxIntervalDays,
                    onUpdate = onUpdate,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
                )
                QuickSetTargetRangeField(
                    yearMonth = yearMonth,
                    span = config.targetSpan,
                    enabled = config.mode != QuickSetMode.MonthlyTotal,
                    onSpanChange = { onUpdate(config.withTargetSpan(it)) },
                    onOpen = onPickerOpen,
                )
                QuickSetFeedback(
                    preview = derivePreview(config, schedule),
                    error = error,
                )
                QuickSetActions(
                    isCompleted = modalState.state is FormState.Completed,
                    canApply = !isBusy && canApply(config, schedule, maxIntervalDays),
                    onCancel = onCancel,
                    onApply = onApply,
                )
            }
        }
    }
}

private fun QuickSetFormState.selectedAmount(): Long = when (mode) {
    QuickSetMode.Daily -> dailyConfig.amount
    QuickSetMode.Weekly -> weeklyConfig.amount
    QuickSetMode.MonthlyTotal -> monthlyConfig.amount
}

/** 换目标区间时按新区间天数上限夹紧日额间隔。 */
private fun QuickSetFormState.withTargetSpan(span: ClosedDateSpan): QuickSetFormState {
    val max = span.maxIntervalDays()
    return copy(
        targetSpan = span,
        dailyConfig = if (dailyConfig.intervalDays > max) dailyConfig.copy(intervalDays = max) else dailyConfig,
    )
}

private fun ClosedDateSpan.maxIntervalDays(): Int = (startDate countDaysTo endDate).toInt() + 1

private fun canApply(
    config: QuickSetFormState,
    schedule: QuickSetSchedule?,
    maxIntervalDays: Int,
): Boolean = schedule != null &&
    config.selectedAmount() > 0L &&
    (config.mode != QuickSetMode.Daily || config.dailyConfig.intervalDays in 1..maxIntervalDays)

private fun derivePreview(config: QuickSetFormState, schedule: QuickSetSchedule?): QuickSetPreview? {
    val plan = schedule ?: return null
    val amount = config.selectedAmount()
    if (amount <= 0L) return null
    val days = plan.segments.sumOf { it.span.length }
    return when (config.mode) {
        QuickSetMode.Daily -> QuickSetPreview.Daily(
            intervalDays = config.dailyConfig.intervalDays,
            amount = amount,
            days = days,
            totalAmount = plan.totalAmount,
        )

        QuickSetMode.Weekly -> QuickSetPreview.Weekly(
            startDayOfWeek = config.weeklyConfig.startDayOfWeek,
            endDayOfWeek = config.weeklyConfig.endDayOfWeek,
            amount = amount,
            days = days,
            totalAmount = plan.totalAmount,
        )

        QuickSetMode.MonthlyTotal -> QuickSetPreview.MonthlyTotal(
            totalAmount = plan.totalAmount,
            days = days,
        )
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
    isCompleted: Boolean,
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
            enabled = !isCompleted,
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
    QuickSetFormError.IntervalInvalid,
    QuickSetFormError.AmountNonPositive,
    is QuickSetFormError.BudgetOverlaps,
    -> null
}

@Composable
private fun Long.asPreviewAmount(): String {
    val prefix = stringResource(R.string.currency_cny_prefix)
    val suffix = stringResource(R.string.currency_cny_suffix)
    return prefix + formatAsAmount(Formatter.AmountConcise) + suffix
}
