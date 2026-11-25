package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.ui.component.ErrorSupportingText
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.FormConfirmActions
import cn.osmrpighead.envelog.ui.component.form.LocalFormEnabled
import cn.osmrpighead.envelog.ui.component.form.ProvideForm
import cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield.OutlinedDateRangeField
import cn.osmrpighead.envelog.ui.component.form.textfield.preset.OutlinedAmountField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetField
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFieldError.AmountNonPositive
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFieldError.BudgetOverlaps
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetForm
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFormError.EnvelopeArchived
import cn.osmrpighead.envelog.ui.screen.envelope.DetailModalState
import cn.osmrpighead.envelog.ui.screen.envelope.component.ledger.AllocationList
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.style.formField
import cn.osmrpighead.envelog.ui.style.formSupportingText
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.ButtonColor
import cn.osmrpighead.envelog.ui.util.hideAndThen

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
        if (detailModalState.dismissed || budgetForm.state == FormState.Completed)
            sheetState.hideAndThen(clear)
        else
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
            BudgetForm(
                budgetForm = budgetForm,
                dismissed = detailModalState.dismissed,
                onSpanChange = { update(BudgetField.DateSpan(it)) },
                onAmountChange = { update(BudgetField.Amount(it)) },
                numKeypadHost = numKeypadHost,
                save = save,
                dismiss = dismiss,
                delete = delete,
                modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space12)),
            )
            AllocationList(
                allocations = detailModalState.allocations,
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.space16),
                    bottom = dimensionResource(R.dimen.space16),
                ),
            )
        }

        NumKeypadHost(
            numKeypadHost,
            modifier = Modifier.disableVerticalDrag(),
        )
    }
}

@Composable
private fun BudgetForm(
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
    val budgetOverlaps = BudgetOverlaps in budgetForm.fieldErrors

    LabeledBox(
        label = stringResource(R.string.envelope_edit_budget_title),
        labelBackground = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier,
    ) {
        ProvideForm(enabled = budgetForm.state is FormState.Active && !dismissed) {
            OutlinedDateRangeField(
                span = budgetForm.budget.dateSpan.toClosed(),
                onSpanChange = { onSpanChange(it.toHalfOpen()) },
                isError = budgetOverlaps,
                supportingText = if (budgetOverlaps) {
                    { Text(text = stringResource(R.string.envelope_edit_msg_budget_overlaps)) }
                } else null,
                label = { Text(stringResource(R.string.envelope_edit_budget_range_label)) },
                modifier = Modifier.formField(),
            )
            OutlinedAmountField(
                amount = budgetForm.budget.amount,
                onAmountChange = onAmountChange,
                host = numKeypadHost,
                nonPositive = AmountNonPositive in budgetForm.fieldErrors,
                currency = CurrencyType.CNY,
                scale = 0,
                format = { formatAsAmount(Formatter.AmountInteger) },
                label = { Text(stringResource(R.string.envelope_edit_budget_amount_label)) },
                modifier = Modifier.formField(),
            )
            if (budgetForm.state is FormState.Active && budgetForm.state.error != null) ErrorSupportingText(
                text = when (budgetForm.state.error) {
                    EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
                },
                modifier = Modifier.formSupportingText(),
            )
            FormConfirmActions(
                onCancel = dismiss,
                onConfirm = {
                    if (numKeypadHost.isOpen) numKeypadHost.safeClose()
                    else save()
                },
                confirmEnabled = !budgetForm.isError,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (!budgetForm.isNew) TextButton(
                    onClick = delete,
                    enabled = LocalFormEnabled.current,
                    colors = ButtonColor.textDanger,
                ) { Text(text = stringResource(R.string.common_action_delete)) }
            }
        }
    }
}
