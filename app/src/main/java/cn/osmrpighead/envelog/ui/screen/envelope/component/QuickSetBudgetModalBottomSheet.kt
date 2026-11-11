package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlanner
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

@Composable
private fun QuickSetMode.label(): String = when (this) {
    QuickSetMode.Daily -> stringResource(R.string.envelope_edit_quick_set_mode_daily)
    QuickSetMode.MonthlyTotal -> stringResource(R.string.envelope_edit_quick_set_mode_total)
}

@Composable
private fun Long.asPreviewAmount(): String {
    val prefix = stringResource(R.string.currency_cny_prefix)
    val suffix = stringResource(R.string.currency_cny_suffix)
    return prefix + formatAsAmount(Formatter.AmountConcise) + suffix
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickSetModeField(
    mode: QuickSetMode,
    onModeChange: (QuickSetMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = mode.label(),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            for (option in QuickSetMode.entries) {
                DropdownMenuItem(
                    text = { Text(text = option.label()) },
                    onClick = {
                        onModeChange(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    today: LocalDate,
    conflict: QuickSetBudgetPlan?,
    error: QuickSetError?,
    onDismiss: () -> Unit,
    onApply: (QuickSetMode, Long) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onCancelOverwrite: () -> Unit,
) {
    val numKeypadHost = rememberNumKeypadHostState()
    val sheetState = rememberModalBottomSheetState()
    val amount = rememberSaveable { mutableStateOf(0L) }
    var mode by rememberSaveable { mutableStateOf(QuickSetMode.Daily) }
    val fieldId = remember { Any() }
    val amountConsumer = remember {
        AmountConsumer(
            scale = 0,
            roundingMode = RoundingMode.HALF_UP,
            submit = { amount.value = it },
            cancel = {},
        )
    }

    LaunchedEffect(sheetState) { sheetState.show() }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == PartiallyExpanded) numKeypadHost.cancel()
    }

    LaunchedEffect(numKeypadHost.isOpen) {
        if (numKeypadHost.isOpen) sheetState.expand()
        else sheetState.partialExpand()
    }

    val span = remember(mode, yearMonth, today) {
        QuickSetBudgetPlanner.targetSpan(mode, yearMonth, today)
    }
    val total = span?.let { QuickSetBudgetPlanner.totalAmount(mode, amount.value, it) }

    ModalBottomSheet(
        onDismissRequest = {
            numKeypadHost.cancel()
            onDismiss()
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
                LabeledBox(
                    label = stringResource(R.string.envelope_edit_quick_set_title),
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
                ) {
                    QuickSetModeField(
                        mode = mode,
                        onModeChange = { mode = it },
                    )
                    OutlinedTextField(
                        value = span?.toClosed()?.format() ?: stringResource(R.string.symbol_percent_nan),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_range_label)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.spacingMedium)),
                    )
                    NumKeypadField(
                        value = amount.value.formatAsAmount(Formatter.AmountInteger),
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
                        label = {
                            Text(
                                text = stringResource(
                                    when (mode) {
                                        QuickSetMode.Daily -> R.string.envelope_edit_quick_set_amount_daily_label
                                        QuickSetMode.MonthlyTotal -> R.string.envelope_edit_quick_set_amount_total_label
                                    },
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.spacingMedium)),
                    )
                    if (amount.value > 0 && span != null) {
                        Text(
                            text = if (total == null) {
                                stringResource(R.string.envelope_edit_quick_set_amount_too_large)
                            } else when (mode) {
                                QuickSetMode.Daily -> stringResource(
                                    R.string.envelope_edit_quick_set_preview_daily,
                                    amount.value.asPreviewAmount(),
                                    span.length,
                                    total.asPreviewAmount(),
                                )

                                QuickSetMode.MonthlyTotal -> stringResource(
                                    R.string.envelope_edit_quick_set_preview_total,
                                    total.asPreviewAmount(),
                                    span.length,
                                )
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (total == null) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
                        )
                    }
                    if (error != null) Text(
                        text = when (error) {
                            QuickSetError.EnvelopeArchived -> stringResource(R.string.envelope_edit_msg_archived)
                            QuickSetError.BudgetOverlaps -> stringResource(R.string.envelope_edit_msg_budget_overlaps)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.spacingMedium)),
                    ) {
                        TextButton(
                            onClick = {
                                numKeypadHost.cancel()
                                onDismiss()
                            },
                        ) {
                            Text(text = stringResource(R.string.common_action_cancel))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (numKeypadHost.isOpen) numKeypadHost.confirm()
                                if (!numKeypadHost.isOpen) onApply(mode, amount.value)
                            },
                            enabled = amount.value > 0 && total != null,
                        ) {
                            Text(text = stringResource(R.string.common_action_ok))
                        }
                    }
                }
            }
        }

        if (conflict != null) {
            val conflictTotal = conflict.conflicts.sumOf { it.amount }
            AlertDialog(
                onDismissRequest = onCancelOverwrite,
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
                    TextButton(onClick = onConfirmOverwrite) {
                        Text(text = stringResource(R.string.envelope_edit_quick_set_conflict_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onCancelOverwrite) {
                        Text(text = stringResource(R.string.common_action_cancel))
                    }
                },
            )
        }
    }
}
