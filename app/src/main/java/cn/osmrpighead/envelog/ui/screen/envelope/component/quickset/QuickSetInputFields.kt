package cn.osmrpighead.envelog.ui.screen.envelope.component.quickset

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
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
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount

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
internal fun QuickSetIntervalField(
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
internal fun QuickSetAmountField(
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
