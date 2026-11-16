package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetField
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFieldError.AmountNonPositive
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFieldError.BudgetOverlaps
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetForm
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFormError.EnvelopeArchived
import cn.osmrpighead.envelog.ui.screen.envelope.DetailModalState
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.ButtonColor
import cn.osmrpighead.envelog.ui.util.nonPositiveToError
import java.math.RoundingMode

@Composable
fun BudgetItemSheet(
    budgetForm: BudgetForm,
    dismissed: Boolean,
    onSpanChange: (HalfOpenDateSpan) -> Unit,
    onAmountChange: (Long) -> Unit,
    numKeypadHost: NumKeypadHostState,
    save: () -> Unit,
    dismiss: () -> Unit,
    delete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldId = remember { Any() }
    val consumer = remember {
        AmountConsumer(
            scale = 0,
            roundingMode = RoundingMode.HALF_UP,
            submit = { onAmountChange(it) },
            cancel = {},
        )
    }

    val enabled = budgetForm.state is FormState.Active && !dismissed

    val budgetOverlaps = BudgetOverlaps in budgetForm.fieldErrors
    val (amountNonPositive, amountError) = consumer.nonPositiveToError(
        AmountNonPositive in budgetForm.fieldErrors,
        fieldId, numKeypadHost,
    )

    LabeledBox(
        label = stringResource(R.string.envelope_edit_budget_title),
        labelBackground = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        DateRangePickerField(
            span = budgetForm.budget.dateSpan.toClosed(),
            dialogBuilder = DateRangePickerDialogBuilder.of(
                onSpanChange = { onSpanChange(it.toHalfOpen()) },
                initialSpan = budgetForm.budget.dateSpan.toClosed(),
            ),
            isError = budgetOverlaps,
            supportingText = if (budgetOverlaps) {
                { Text(text = stringResource(R.string.envelope_edit_msg_budget_overlaps)) }
            } else null,
            enabled = enabled,
            label = { Text(stringResource(R.string.envelope_edit_budget_range_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NumKeypadField(
            value = budgetForm.budget.amount.formatAsAmount(Formatter.AmountInteger),
            host = numKeypadHost,
            consumer = consumer,
            fieldId = fieldId,
            isError = amountError,
            supportingText = if (amountNonPositive) {
                { Text(text = stringResource(R.string.common_msg_amount_positive)) }
            } else null,
            enabled = enabled,
            label = { Text(stringResource(R.string.envelope_edit_budget_amount_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacing4)),
        )
        if (budgetForm.state is FormState.Active && budgetForm.state.error != null) Text(
            text = when (budgetForm.state.error) {
                EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacing4)),
        ) {
            if (!budgetForm.isNew) TextButton(
                onClick = delete,
                enabled = enabled,
                colors = ButtonColor.outlinedDanger,
            ) {
                Text(text = stringResource(R.string.common_action_delete))
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = dismiss,
                enabled = enabled,
            ) {
                Text(text = stringResource(R.string.common_action_cancel))
            }
            Button(
                onClick = {
                    if (numKeypadHost.isOpen) numKeypadHost.safeClose()
                    else save()
                },
                enabled = enabled && !budgetForm.isError,
            ) { Text(text = stringResource(R.string.common_action_ok)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailModalBottomSheet(
    detailModalState: DetailModalState,
    update: (BudgetField) -> Unit,
    dismiss: () -> Unit,
    save: () -> Unit,
    delete: () -> Unit,
    clear: () -> Unit,
) {
    val budgetForm by rememberUpdatedState(detailModalState.budgetForm)

    val numKeypadHost = rememberNumKeypadHostState()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(sheetState) { sheetState.show() }

    LaunchedEffect(sheetState.currentValue) {
        when (sheetState.currentValue) {
            PartiallyExpanded -> numKeypadHost.cancel()
            else -> {}
        }
    }

    LaunchedEffect(numKeypadHost.isOpen, budgetForm.state, detailModalState.dismissed) {
        if (detailModalState.dismissed || budgetForm.state == FormState.Completed) {
            try {
                sheetState.hide()
            } finally {
                clear()
            }
        } else
            if (numKeypadHost.isOpen) sheetState.expand()
            else sheetState.partialExpand()
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (budgetForm.saveBeforeQuit) save()
            else dismiss()
        },
        sheetState = sheetState,
        sheetGesturesEnabled = !sheetState.isAnimationRunning,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Text(
                text = detailModalState.dateSpan.format(),
                color = MaterialTheme.colorScheme.onSecondaryFixedVariant,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            BudgetItemSheet(
                budgetForm = budgetForm,
                dismissed = detailModalState.dismissed,
                onSpanChange = { update(BudgetField.DateSpan(it)) },
                onAmountChange = { update(BudgetField.Amount(it)) },
                numKeypadHost = numKeypadHost,
                save = save,
                dismiss = dismiss,
                delete = delete,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacing6)),
            )
            AllocationList(
                allocations = detailModalState.allocations,
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.spacing8),
                    bottom = dimensionResource(R.dimen.spacing8),
                ),
            )
        }

        NumKeypadHost(
            numKeypadHost,
            modifier = Modifier.disableVerticalDrag(),
        )
    }
}
