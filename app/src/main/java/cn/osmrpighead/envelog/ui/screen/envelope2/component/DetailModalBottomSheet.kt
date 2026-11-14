package cn.osmrpighead.envelog.ui.screen.envelope2.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.component.AllocationList
import cn.osmrpighead.envelog.ui.screen.envelope2.BudgetForm
import cn.osmrpighead.envelog.ui.screen.envelope2.BudgetFormError.*
import cn.osmrpighead.envelog.ui.screen.envelope2.DetailModalState
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import java.math.RoundingMode

@Composable
fun BudgetItemSheet(
    budgetForm: BudgetForm,
    onSpanChange: (HalfOpenDateSpan) -> Unit,
    numKeypadHost: NumKeypadHostState,
    amountConsumer: AmountConsumer,
    save: () -> Unit,
    cancel: () -> Unit,
    delete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldId = remember { Any() }

    LabeledBox(
        label = stringResource(R.string.envelope_edit_budget_title),
        modifier = modifier,
    ) {
        DateRangePickerField(
            span = budgetForm.budget.dateSpan.toClosed(),
            dialogBuilder = DateRangePickerDialogBuilder.Default(
                DateRangePickerConfirmEventHandler.Default { onSpanChange(it.toHalfOpen()) },
            ),
            label = { Text(stringResource(R.string.envelope_edit_budget_range_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NumKeypadField(
            value = budgetForm.budget.amount.formatAsAmount(Formatter.AmountInteger),
            host = numKeypadHost,
            consumer = amountConsumer,
            fieldId = fieldId,
            isError = amountConsumer.validationError != null,
            supportingText = when (amountConsumer.validationError) {
                null -> null
                is AmountValidationError.MathError -> null
                AmountValidationError.NonPositive -> {
                    { Text(text = stringResource(R.string.common_msg_amount_positive)) }
                }
            },
            label = { Text(stringResource(R.string.envelope_edit_budget_amount_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium)),
        )
        if (budgetForm.state is FormState.Active && budgetForm.state.error != null) Text(
            text = when (budgetForm.state.error) {
                EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
                BudgetOverlaps -> stringResource(R.string.envelope_edit_msg_budget_overlaps)
                AmountNonPositive -> stringResource(R.string.common_msg_amount_positive)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium)),
        ) {
            TextButton(onClick = delete) {
                Text(text = stringResource(R.string.common_action_delete), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = cancel) {
                Text(text = stringResource(R.string.common_action_cancel))
            }
            Button(
                onClick = {
                    numKeypadHost.confirm()
                    save()
                },
                enabled = budgetForm.state is FormState.Active && budgetForm.state.error == null,
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailModalBottomSheet(
    detailModalState: DetailModalState,
    update: (Budget) -> Unit,
    close: () -> Unit,
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

    LaunchedEffect(numKeypadHost.isOpen) {
        if (numKeypadHost.isOpen) sheetState.expand()
        else sheetState.partialExpand()
    }

    LaunchedEffect(budgetForm.state, detailModalState.dismissed) {
        if (budgetForm.state != FormState.Completed && !detailModalState.dismissed) return@LaunchedEffect
        if (sheetState.isVisible) sheetState.hide()
        clear()
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (budgetForm.saveBeforeQuit) save()
            else clear()
        },
        sheetState = sheetState,
        sheetGesturesEnabled = !sheetState.isAnimationRunning,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            BudgetItemSheet(
                budgetForm = budgetForm,
                onSpanChange = { update(budgetForm.budget.copy(dateSpan = it)) },
                numKeypadHost = numKeypadHost,
                amountConsumer = AmountConsumer(
                    scale = 0,
                    roundingMode = RoundingMode.HALF_UP,
                    submit = { update(budgetForm.budget.copy(amount = it)) },
                    cancel = {},
                ),
                save = save,
                cancel = close,
                delete = delete,
                modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
            )
            AllocationList(
                allocations = detailModalState.allocations,
                modifier = Modifier.padding(
                    top = dimensionResource(R.dimen.spacingMedium),
                    bottom = dimensionResource(R.dimen.spacingBold),
                ),
            )
        }

        NumKeypadHost(
            numKeypadHost,
            modifier = Modifier.disableVerticalDrag(),
        )
    }
}
