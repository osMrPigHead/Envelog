package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.ConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveBudget
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveError
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveError.*
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import java.math.RoundingMode

@Composable
fun BudgetItemSheet(
    budget: Budget,
    allocations: List<AllocationWithInfo>,
    onSpanChange: (HalfOpenDateSpan) -> Unit,
    numKeypadHost: NumKeypadHostState,
    amountConsumer: AmountConsumer,
    fieldId: Any,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    error: ActiveError?,
    modifier: Modifier = Modifier,
) {
    LabeledBox(
        label = stringResource(R.string.envelope_edit_budget_title),
        modifier = modifier,
    ) {
        DateRangePickerField(
            span = budget.dateSpan.toClosed(),
            dialogBuilder = DateRangePickerDialogBuilder.Default(
                ConfirmEventHandler.Default { onSpanChange(it.toHalfOpen()) },
            ),
            label = { Text(stringResource(R.string.envelope_edit_budget_range_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        NumKeypadField(
            value = budget.amount.formatAsAmount(Formatter.AmountInteger),
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
        if (error != null) Text(
            text = when (error) {
                EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
                BudgetOverlaps -> stringResource(R.string.envelope_edit_msg_budget_overlaps)
                AmountNonPositive -> stringResource(R.string.common_msg_amount_positive)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        AllocationList(
            allocations = allocations,
            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium)),
        ) {
            TextButton(onClick = onDelete) {
                Text(text = stringResource(R.string.common_action_delete), color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.common_action_cancel))
            }
            Button(
                onClick = {
                    numKeypadHost.confirm()
                    onSave()
                },
                enabled = error == null,
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetItemModalSheet(
    activeBudget: ActiveBudget,
    amountFieldId: Any,
    update: (Budget) -> Unit,
    close: () -> Unit,
    save: () -> Unit,
    delete: () -> Unit,
    clear: () -> Unit,
) {
    if (activeBudget.budget != null) {
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

        LaunchedEffect(activeBudget.error, sheetState.currentValue) {
            if (activeBudget.error != null && sheetState.currentValue == SheetValue.Hidden)
                sheetState.show()
        }

        LaunchedEffect(activeBudget.complete) {
            if (!activeBudget.complete) return@LaunchedEffect
            sheetState.hide()
            clear()
        }

        ModalBottomSheet(
            onDismissRequest = {
                numKeypadHost.confirm()
                save()
            },
            sheetState = sheetState,
        ) {
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
                        .verticalScroll(rememberScrollState()),
                ) {
                    BudgetItemSheet(
                        budget = activeBudget.budget,
                        allocations = activeBudget.allocations,
                        onSpanChange = { update(activeBudget.budget.copy(dateSpan = it)) },
                        numKeypadHost = numKeypadHost,
                        amountConsumer = AmountConsumer(
                            scale = 0,
                            roundingMode = RoundingMode.HALF_UP,
                            submit = { update(activeBudget.budget.copy(amount = it)) },
                            cancel = {},
                        ),
                        fieldId = amountFieldId,
                        onSave = save,
                        onCancel = close,
                        onDelete = delete,
                        error = activeBudget.error,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
                    )
                }
            }
        }
    }
}
