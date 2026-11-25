package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.ui.component.form.RadioRow
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextField
import cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield.OutlinedDayOfWeekRangeField
import cn.osmrpighead.envelog.ui.component.form.textfield.preset.FieldPrefix
import cn.osmrpighead.envelog.ui.component.form.textfield.preset.FieldSuffix
import cn.osmrpighead.envelog.ui.component.form.textfield.preset.OutlinedAmountField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFieldError
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.style.formRow
import cn.osmrpighead.envelog.ui.style.inlineFormField
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@Composable
fun QuickSetModeSection(
    currency: CurrencyType,
    enabled: Boolean,
    config: QuickSetFormState,
    fieldErrors: Set<QuickSetFieldError>,
    numKeypadHost: NumKeypadHostState,
    maxIntervalDays: Long,
    update: (QuickSetField) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.selectableGroup()) {
        QuickSetModeRow(
            config.mode, enabled, fieldErrors, numKeypadHost, update, config.dailyConfig.amount, currency,
            amountField = QuickSetField.Daily::Amount,
            rowMode = QuickSetMode.Daily,
            modifier = Modifier.formRow(),
        ) { amountField ->
            val intervalInvalid = QuickSetFieldError.IntervalInvalid in fieldErrors
            IntervalField(
                days = config.dailyConfig.intervalDays,
                maxDays = maxIntervalDays,
                onValueChange = { days -> update(QuickSetField.Daily.Interval(days)) },
                isError = intervalInvalid,
                supportingText = if (intervalInvalid) {
                    { Text(text = stringResource(R.string.envelope_edit_quick_set_interval_invalid)) }
                } else null,
                modifier = Modifier.weight(1f).inlineFormField(),
            )
            amountField {}
        }

        QuickSetModeRow(
            config.mode, enabled, fieldErrors, numKeypadHost, update, config.weeklyConfig.amount, currency,
            amountField = QuickSetField.Weekly::Amount,
            rowMode = QuickSetMode.Weekly,
            modifier = Modifier.formRow(),
        ) { amountField ->
            OutlinedDayOfWeekRangeField(
                startDayOfWeek = config.weeklyConfig.startDayOfWeek,
                endDayOfWeek = config.weeklyConfig.endDayOfWeek,
                onDayOfWeekRangeChange = { start, end -> update(QuickSetField.Weekly.DaySpan(start, end)) },
                textStyle = EnvelogTheme.typography.fieldMedium,
                label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_weekly_label)) },
                prefix = { FieldPrefix(text = stringResource(R.string.envelope_edit_quick_set_interval_prefix)) },
                modifier = Modifier.weight(1f).inlineFormField(),
            )
            amountField {}
        }

        QuickSetModeRow(
            config.mode, enabled, fieldErrors, numKeypadHost, update, config.monthlyConfig.amount, currency,
            amountField = QuickSetField.Monthly::Amount,
            rowMode = QuickSetMode.Monthly,
            modifier = Modifier.formRow(),
        ) { amountField ->
            amountField { Text(text = stringResource(R.string.envelope_edit_quick_set_monthly_amount_field)) }
        }
    }
}

@Composable
private fun QuickSetModeRow(
    mode: QuickSetMode,
    enabled: Boolean,
    fieldErrors: Set<QuickSetFieldError>,
    numKeypadHost: NumKeypadHostState,
    update: (QuickSetField) -> Unit,
    amount: Long,
    currency: CurrencyType,
    amountField: (Long) -> QuickSetField,
    rowMode: QuickSetMode,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(amountField: @Composable (label: @Composable () -> Unit) -> Unit) -> Unit,
) {
    RadioRow(
        selected = mode == rowMode,
        enabled = enabled,
        onClick = { update(QuickSetField.Mode(rowMode)) },
        modifier = modifier,
    ) {
        content { label ->
            OutlinedAmountField(
                amount = amount,
                onAmountChange = { amount -> update(amountField(amount)) },
                host = numKeypadHost,
                currency = currency,
                nonPositive = QuickSetFieldError.AmountNonPositive in fieldErrors,
                scale = 0,
                format = { formatAsAmount(Formatter.AmountInteger) },
                label = label,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).inlineFormField(),
            )
        }
    }
}

@Composable
private fun IntervalField(
    days: Int,
    maxDays: Long,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val text = days.toString()

    EnvelogOutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { input -> input.sanitizedIntervalDays(maxDays)?.let(onValueChange) },
        isError = isError,
        supportingText = supportingText,
        label = { Text(text = stringResource(R.string.envelope_edit_quick_set_mode_daily_label)) },
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        prefix = { FieldPrefix(text = stringResource(R.string.envelope_edit_quick_set_interval_prefix)) },
        suffix = { FieldSuffix(text = pluralStringResource(R.plurals.time_plurals_day, count = days)) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = modifier,
    )
}

private fun TextFieldValue.sanitizedIntervalDays(maxDays: Long): Int? {
    if (text.isEmpty()) return 0
    if (text.any { it !in '0'..'9' }) return null

    val days = text.trimStart('0').ifEmpty { "0" }.toIntOrNull() ?: return null
    return days.takeUnless { it > maxDays }
}
