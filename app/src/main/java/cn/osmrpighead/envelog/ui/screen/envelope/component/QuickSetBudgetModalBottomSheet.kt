package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.ConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerField
import cn.osmrpighead.envelog.ui.component.form.DayOfWeekRangePickerSheetBuilder
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlanner
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
private fun Long.asPreviewAmount(): String {
    val prefix = stringResource(R.string.currency_cny_prefix)
    val suffix = stringResource(R.string.currency_cny_suffix)
    return prefix + formatAsAmount(Formatter.AmountConcise) + suffix
}

@Composable
private fun QuickSetAffix(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun QuickSetModeRow(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(vertical = dimensionResource(R.dimen.spacingMini)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingSmall)),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            RadioButton(selected = selected, onClick = null)
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingSmall)),
                verticalAlignment = Alignment.Bottom,
            ) {
                content()
            }
        },
    )
}

/**
 * 只接受纯数字输入，符号一律拒绝。
 * 输入为空（退格清空）时回到 0，光标位于 0 之后；超出目标区间天数的输入同样拒绝。
 */
private fun TextFieldValue.sanitizeIntervalInput(maxDays: Int): TextFieldValue? {
    if (text.isEmpty()) return TextFieldValue("0", selection = TextRange(1))
    if (text.any { it !in '0'..'9' }) return null

    val normalized = text.trimStart('0').ifEmpty { "0" }
    val days = normalized.toIntOrNull() ?: return null
    if (days > maxDays) return null

    return TextFieldValue(normalized, selection = TextRange(normalized.length))
}

@Composable
private fun QuickSetIntervalField(
    value: TextFieldValue,
    maxDays: Int,
    enabled: Boolean,
    description: String,
    onValueChange: (TextFieldValue) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = value,
        onValueChange = { input -> input.sanitizeIntervalInput(maxDays)?.let(onValueChange) },
        modifier = modifier
            .semantics { this.contentDescription = description }
            .onFocusChanged { if (it.isFocused) onFocus() },
        enabled = enabled,
        singleLine = true,
        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_daily_label)) },
        prefix = {
            QuickSetAffix(
                text = stringResource(R.string.envelope_edit_quick_set_interval_prefix),
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingThin)),
            )
        },
        suffix = {
            QuickSetAffix(
                text = stringResource(R.string.envelope_edit_quick_set_interval_suffix),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingThin)),
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
    )
}

@Composable
private fun QuickSetAmountField(
    amount: Long,
    host: NumKeypadHostState,
    consumer: AmountConsumer,
    fieldId: Any,
    enabled: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val labelContent: (@Composable () -> Unit)? = if (label == null) null else {
        { Text(text = label) }
    }

    NumKeypadField(
        value = amount.formatAsAmount(Formatter.AmountInteger),
        host = host,
        consumer = consumer,
        fieldId = fieldId,
        enabled = enabled,
        label = labelContent,
        suffix = {
            QuickSetAffix(
                text = stringResource(R.string.envelope_edit_quick_set_amount_unit),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingThin)),
            )
        },
        modifier = modifier.semantics { this.contentDescription = description },
    )
}

@Composable
private fun QuickSetModeSection(
    mode: QuickSetMode,
    onModeChange: (QuickSetMode) -> Unit,
    numKeypadHost: NumKeypadHostState,
    intervalValue: TextFieldValue,
    onIntervalValueChange: (TextFieldValue) -> Unit,
    maxIntervalDays: Int,
    onIntervalFocus: () -> Unit,
    dailyAmount: Long,
    dailyAmountConsumer: AmountConsumer,
    dailyAmountFieldId: Any,
    weeklyStartDayOfWeek: DayOfWeek,
    weeklyEndDayOfWeek: DayOfWeek,
    onWeeklyRangeChange: (DayOfWeek, DayOfWeek) -> Unit,
    weeklyAmount: Long,
    weeklyAmountConsumer: AmountConsumer,
    weeklyAmountFieldId: Any,
    monthlyAmount: Long,
    monthlyAmountConsumer: AmountConsumer,
    monthlyAmountFieldId: Any,
    modifier: Modifier = Modifier,
) {
    val dailySelected = mode == QuickSetMode.Daily
    val weeklySelected = mode == QuickSetMode.Weekly
    val monthlySelected = mode == QuickSetMode.MonthlyTotal
    val weekdayDescription = stringResource(R.string.envelope_edit_quick_set_weekday_range_field)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier.selectableGroup(),
    ) {
        QuickSetModeRow(
            selected = dailySelected,
            onSelect = { onModeChange(QuickSetMode.Daily) },
        ) {
            QuickSetIntervalField(
                value = intervalValue,
                maxDays = maxIntervalDays,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_interval_field),
                onValueChange = onIntervalValueChange,
                onFocus = onIntervalFocus,
                modifier = Modifier.weight(1f),
            )
            QuickSetAmountField(
                amount = dailyAmount,
                host = numKeypadHost,
                consumer = dailyAmountConsumer,
                fieldId = dailyAmountFieldId,
                enabled = dailySelected,
                description = stringResource(R.string.envelope_edit_quick_set_daily_amount_field),
                modifier = Modifier.weight(1f),
            )
        }

        QuickSetModeRow(
            selected = weeklySelected,
            onSelect = { onModeChange(QuickSetMode.Weekly) },
        ) {
            DayOfWeekRangePickerField(
                startDayOfWeek = weeklyStartDayOfWeek,
                endDayOfWeek = weeklyEndDayOfWeek,
                sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                    confirmEventHandler = DayOfWeekRangePickerConfirmEventHandler.Default(onWeeklyRangeChange),
                ),
                enabled = weeklySelected,
                textStyle = MaterialTheme.typography.bodyMedium,
                label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_weekly_label)) },
                onOpen = {
                    numKeypadHost.safeClose()
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { this.contentDescription = weekdayDescription },
            )
            QuickSetAmountField(
                amount = weeklyAmount,
                host = numKeypadHost,
                consumer = weeklyAmountConsumer,
                fieldId = weeklyAmountFieldId,
                enabled = weeklySelected,
                description = stringResource(R.string.envelope_edit_quick_set_weekly_amount_field),
                modifier = Modifier.weight(1f),
            )
        }

        QuickSetModeRow(
            selected = monthlySelected,
            onSelect = { onModeChange(QuickSetMode.MonthlyTotal) },
        ) {
            QuickSetAmountField(
                amount = monthlyAmount,
                host = numKeypadHost,
                consumer = monthlyAmountConsumer,
                fieldId = monthlyAmountFieldId,
                enabled = monthlySelected,
                description = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                label = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field),
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSetBudgetModalBottomSheet(
    yearMonth: YearMonth,
    complete: Boolean,
    conflict: QuickSetBudgetPlan?,
    error: QuickSetError?,
    onDismissRequest: () -> Unit,
    onDismissed: () -> Unit,
    onApply: (QuickSetConfig, ClosedDateSpan) -> Unit,
    onConfirmOverwrite: () -> Unit,
    onCancelOverwrite: () -> Unit,
) {
    val numKeypadHost = rememberNumKeypadHostState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val defaultTarget = remember(yearMonth) { yearMonth.toHalfOpenDateSpan().toClosed() }

    var modeName by rememberSaveable { mutableStateOf(QuickSetMode.Daily.name) }
    val mode = QuickSetMode.valueOf(modeName)
    var intervalFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue("1", TextRange(1)))
    }
    val intervalDays = intervalFieldValue.text.toIntOrNull() ?: 0
    var dailyAmount by rememberSaveable { mutableLongStateOf(0L) }
    var weeklyStartName by rememberSaveable { mutableStateOf(DayOfWeek.MONDAY.name) }
    var weeklyEndName by rememberSaveable { mutableStateOf(DayOfWeek.SUNDAY.name) }
    var weeklyAmount by rememberSaveable { mutableLongStateOf(0L) }
    var monthlyAmount by rememberSaveable { mutableLongStateOf(0L) }
    var targetStartEpochDay by rememberSaveable { mutableLongStateOf(defaultTarget.startDate.toEpochDay()) }
    var targetEndEpochDay by rememberSaveable { mutableLongStateOf(defaultTarget.endDate.toEpochDay()) }

    val weeklyStartDayOfWeek = DayOfWeek.valueOf(weeklyStartName)
    val weeklyEndDayOfWeek = DayOfWeek.valueOf(weeklyEndName)
    val targetSpan = remember(targetStartEpochDay, targetEndEpochDay) {
        ClosedDateSpan(
            startDate = LocalDate.ofEpochDay(targetStartEpochDay),
            endDate = LocalDate.ofEpochDay(targetEndEpochDay),
        )
    }
    val maxIntervalDays = (targetSpan.startDate countDaysTo targetSpan.endDate).toInt() + 1

    val dailyAmountConsumer = remember {
        AmountConsumer(scale = 0, roundingMode = RoundingMode.HALF_UP, submit = { dailyAmount = it }, cancel = {})
    }
    val weeklyAmountConsumer = remember {
        AmountConsumer(scale = 0, roundingMode = RoundingMode.HALF_UP, submit = { weeklyAmount = it }, cancel = {})
    }
    val monthlyAmountConsumer = remember {
        AmountConsumer(scale = 0, roundingMode = RoundingMode.HALF_UP, submit = { monthlyAmount = it }, cancel = {})
    }
    val dailyAmountFieldId = remember { Any() }
    val weeklyAmountFieldId = remember { Any() }
    val monthlyAmountFieldId = remember { Any() }

    val config = when (mode) {
        QuickSetMode.Daily -> QuickSetConfig.Daily(intervalDays, dailyAmount)
        QuickSetMode.Weekly -> QuickSetConfig.Weekly(weeklyStartDayOfWeek, weeklyEndDayOfWeek, weeklyAmount)
        QuickSetMode.MonthlyTotal -> QuickSetConfig.MonthlyTotal(monthlyAmount)
    }
    val buildConfig: () -> QuickSetConfig = {
        when (mode) {
            QuickSetMode.Daily -> QuickSetConfig.Daily(intervalDays, dailyAmount)
            QuickSetMode.Weekly -> QuickSetConfig.Weekly(
                DayOfWeek.valueOf(weeklyStartName),
                DayOfWeek.valueOf(weeklyEndName),
                weeklyAmount,
            )

            QuickSetMode.MonthlyTotal -> QuickSetConfig.MonthlyTotal(monthlyAmount)
        }
    }
    val selectedAmountConsumer = when (mode) {
        QuickSetMode.Daily -> dailyAmountConsumer
        QuickSetMode.Weekly -> weeklyAmountConsumer
        QuickSetMode.MonthlyTotal -> monthlyAmountConsumer
    }
    val schedule = remember(config, targetSpan, yearMonth) {
        QuickSetBudgetPlanner.schedule(config, targetSpan, yearMonth)
    }
    val selectableDates = remember(yearMonth) {
        val monthSpan = yearMonth.toHalfOpenDateSpan()
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis.toLocalDate() in monthSpan

            override fun isSelectableYear(year: Int): Boolean = year == yearMonth.year
        }
    }

    val intervalValid = mode != QuickSetMode.Daily || intervalDays in 1..maxIntervalDays
    val amountValid = selectedAmountConsumer.validationError == null && config.amount > 0
    val canApply = !complete && intervalValid && amountValid && schedule != null
    val validationMessage = when {
        mode == QuickSetMode.Daily && !intervalValid ->
            stringResource(R.string.envelope_edit_quick_set_interval_invalid)

        selectedAmountConsumer.validationError is AmountValidationError.NonPositive ->
            stringResource(R.string.common_msg_amount_positive)

        config.amount > 0 && schedule == null ->
            stringResource(R.string.envelope_edit_quick_set_cannot_generate)

        else -> null
    }
    val preview = if (config.amount > 0 && schedule != null) {
        val total = schedule.totalAmount.asPreviewAmount()
        val days = schedule.segments.sumOf { it.span.length }
        when (mode) {
            QuickSetMode.Daily -> stringResource(
                R.string.envelope_edit_quick_set_preview_daily,
                intervalDays,
                config.amount.asPreviewAmount(),
                days,
                total,
            )

            QuickSetMode.Weekly -> stringResource(
                R.string.envelope_edit_quick_set_preview_weekly,
                dayOfWeekRangeText(weeklyStartDayOfWeek, weeklyEndDayOfWeek),
                config.amount.asPreviewAmount(),
                days,
                total,
            )

            QuickSetMode.MonthlyTotal -> stringResource(
                R.string.envelope_edit_quick_set_preview_monthly,
                total,
                days,
            )
        }
    } else null

    val selectMode: (QuickSetMode) -> Unit = { newMode ->
        if (newMode.name != modeName) {
            numKeypadHost.safeClose()
            focusManager.clearFocus()
            modeName = newMode.name
        }
    }

    val intervalFocus: () -> Unit = {
        numKeypadHost.safeClose()
        scope.launch { sheetState.expand() }
    }

    LaunchedEffect(sheetState) { sheetState.show() }

    LaunchedEffect(targetSpan) {
        if (intervalDays > maxIntervalDays) {
            val text = maxIntervalDays.toString()
            intervalFieldValue = TextFieldValue(text, TextRange(text.length))
        }
    }

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == PartiallyExpanded) {
            numKeypadHost.cancel()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(complete) {
        if (!complete) return@LaunchedEffect
        numKeypadHost.cancel()
        sheetState.hide()
        onDismissed()
    }

    ModalBottomSheet(
        onDismissRequest = {
            numKeypadHost.cancel()
            onDismissRequest()
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
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                LabeledBox(
                    label = stringResource(R.string.envelope_edit_quick_set_title),
                    modifier = Modifier.padding(dimensionResource(R.dimen.spacingLarge)),
                ) {
                    QuickSetModeSection(
                        mode = mode,
                        onModeChange = selectMode,
                        numKeypadHost = numKeypadHost,
                        intervalValue = intervalFieldValue,
                        onIntervalValueChange = { intervalFieldValue = it },
                        maxIntervalDays = maxIntervalDays,
                        onIntervalFocus = intervalFocus,
                        dailyAmount = dailyAmount,
                        dailyAmountConsumer = dailyAmountConsumer,
                        dailyAmountFieldId = dailyAmountFieldId,
                        weeklyStartDayOfWeek = weeklyStartDayOfWeek,
                        weeklyEndDayOfWeek = weeklyEndDayOfWeek,
                        onWeeklyRangeChange = { start, end ->
                            weeklyStartName = start.name
                            weeklyEndName = end.name
                        },
                        weeklyAmount = weeklyAmount,
                        weeklyAmountConsumer = weeklyAmountConsumer,
                        weeklyAmountFieldId = weeklyAmountFieldId,
                        monthlyAmount = monthlyAmount,
                        monthlyAmountConsumer = monthlyAmountConsumer,
                        monthlyAmountFieldId = monthlyAmountFieldId,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
                    )
                    DateRangePickerField(
                        span = targetSpan,
                        dialogBuilder = DateRangePickerDialogBuilder.Default(
                            confirmEventHandler = ConfirmEventHandler.Default { span ->
                                targetStartEpochDay = span.startDate.toEpochDay()
                                targetEndEpochDay = span.endDate.toEpochDay()
                            },
                            initialSpan = targetSpan,
                            selectableDates = selectableDates,
                        ),
                        enabled = mode != QuickSetMode.MonthlyTotal,
                        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_range_label)) },
                        onOpen = {
                            numKeypadHost.safeClose()
                            focusManager.clearFocus()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = dimensionResource(R.dimen.spacingMedium)),
                    )
                    if (preview != null) Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
                    )
                    if (validationMessage != null) Text(
                        text = validationMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingSmall)),
                    )
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
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = {
                                numKeypadHost.cancel()
                                onDismissRequest()
                            },
                            enabled = !complete,
                        ) {
                            Text(text = stringResource(R.string.common_action_cancel))
                        }
                        Button(
                            onClick = {
                                if (numKeypadHost.isOpen) numKeypadHost.confirm()
                                else {
                                    focusManager.clearFocus()
                                    onApply(buildConfig(), targetSpan)
                                }
                            },
                            enabled = canApply,
                        ) {
                            Text(text = stringResource(R.string.common_action_ok))
                        }
                    }
                }
            }
        }

        if (conflict != null && !complete) {
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
