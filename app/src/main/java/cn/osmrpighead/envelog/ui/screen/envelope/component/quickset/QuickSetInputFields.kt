package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFieldError
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp
import java.math.RoundingMode
import java.time.YearMonth

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

/**
 * 只接受纯数字输入，符号一律拒绝；超出目标区间天数的输入同样拒绝。
 * 返回 null 表示本次输入无效，应保持原值。
 */
private fun TextFieldValue.sanitizedIntervalDays(maxDays: Long): Long? {
    if (text.isEmpty()) return 0
    if (text.any { it !in '0'..'9' }) return null

    val days = text.trimStart('0').ifEmpty { "0" }.toLongOrNull() ?: return null
    return days.takeUnless { it > maxDays }
}

@Composable
internal fun QuickSetIntervalField(
    days: Long,
    maxDays: Long,
    enabled: Boolean,
    description: String,
    onValueChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
) {
    val focusManager = LocalFocusManager.current
    val text = days.toString()

    OutlinedTextField(
        value = TextFieldValue(text, TextRange(text.length)),
        onValueChange = { input -> input.sanitizedIntervalDays(maxDays)?.let(onValueChange) },
        modifier = modifier.semantics { this.contentDescription = description },
        enabled = enabled,
        singleLine = true,
        isError = isError,
        supportingText = supportingText,
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
internal fun QuickSetAmountField(
    amount: Long,
    fieldErrors: Set<QuickSetFieldError>,
    host: NumKeypadHostState,
    update: (Long) -> Unit,
    selected: Boolean,
    description: String,
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    val labelContent: (@Composable () -> Unit)? = if (label == null) null else {
        { Text(text = label) }
    }

    val fieldId = remember { Any() }
    val consumer = remember {
        AmountConsumer(
            scale = 0,
            roundingMode = RoundingMode.HALF_UP,
            submit = update,
            cancel = {},
        )
    }

    val active = host.activeFieldId == fieldId
    val nonPositive = QuickSetFieldError.AmountNonPositive in fieldErrors ||
            (active && consumer.validationError == AmountValidationError.NonPositive)
    val error = nonPositive || (active && consumer.validationError != null)

    NumKeypadField(
        value = amount.formatAsAmount(Formatter.AmountInteger),
        host = host,
        consumer = consumer,
        fieldId = fieldId,
        enabled = selected,
        isError = error && selected,
        supportingText =
            if (nonPositive && selected) {
                { Text(text = stringResource(R.string.common_msg_amount_positive)) }
            }
            else {
                { Spacer(Modifier.height(MaterialTheme.typography.bodySmall.lineHeightAsDp)) }
            },
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
internal fun QuickSetTargetRangeField(
    yearMonth: YearMonth,
    span: ClosedDateSpan,
    enabled: Boolean,
    onSpanChange: (ClosedDateSpan) -> Unit,
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacingMedium)),
    )
}
