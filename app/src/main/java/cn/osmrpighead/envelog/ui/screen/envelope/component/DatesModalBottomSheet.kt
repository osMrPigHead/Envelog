package cn.osmrpighead.envelog.ui.screen.envelope.component

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
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
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveBudgetError
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveBudgetError.*
import cn.osmrpighead.envelog.ui.screen.envelope.ActiveItem
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import java.math.RoundingMode

@Composable
fun BudgetItemSheet(
    budget: Budget,
    onSpanChange: (HalfOpenDateSpan) -> Unit,
    numKeypadHost: NumKeypadHostState,
    amountConsumer: AmountConsumer,
    fieldId: Any,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    error: ActiveBudgetError?,
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
fun DatesModalBottomSheet(
    activeItem: ActiveItem,
    amountFieldId: Any,
    update: (Budget) -> Unit,
    close: () -> Unit,
    save: () -> Unit,
    delete: () -> Unit,
    clear: () -> Unit,
) {
    if (activeItem.budget != null) {
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

        LaunchedEffect(activeItem.error, sheetState.currentValue) {
            if (!activeItem.isNew && activeItem.error != null && sheetState.currentValue == SheetValue.Hidden)
                sheetState.show()
        }

        LaunchedEffect(activeItem.complete) {
            if (!activeItem.complete) return@LaunchedEffect
            sheetState.hide()
            clear()
        }

        ModalBottomSheet(
            onDismissRequest = {
                if (activeItem.isNew && activeItem.error != null) close()
                else save()
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
                        budget = activeItem.budget,
                        onSpanChange = { update(activeItem.budget.copy(dateSpan = it)) },
                        numKeypadHost = numKeypadHost,
                        amountConsumer = AmountConsumer(
                            scale = 0,
                            roundingMode = RoundingMode.HALF_UP,
                            submit = { update(activeItem.budget.copy(amount = it)) },
                            cancel = {},
                        ),
                        fieldId = amountFieldId,
                        onSave = save,
                        onCancel = close,
                        onDelete = delete,
                        error = activeItem.error,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
                    )
                    AllocationList(
                        allocations = activeItem.allocations,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.spacingMedium),
                            bottom = dimensionResource(R.dimen.spacingBold),
                        ),
                    )
                }
            }
        }
    }
}
