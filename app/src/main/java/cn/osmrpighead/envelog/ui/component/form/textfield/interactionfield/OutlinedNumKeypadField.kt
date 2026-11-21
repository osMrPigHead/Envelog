package cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.LocalEnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState

@Composable
fun OutlinedNumKeypadField(
    value: String,
    host: NumKeypadHostState,
    consumer: NumKeypadConsumer,
    modifier: Modifier = Modifier,
    fieldId: Any = remember { Any() },
    enabled: Boolean = true,
    style: EnvelogOutlinedTextFieldStyle = LocalEnvelogOutlinedTextFieldStyle.current,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
) {
    val consumer by rememberUpdatedState(consumer)

    OutlinedInteractionField(
        value = if (host.activeFieldId == fieldId) host.state.text else value,
        onClick = {
            if (host.activeFieldId != fieldId) host.request(fieldId, consumer, value)
        },
        modifier = modifier.onFocusChanged {
            if (!it.isFocused) host.safeClose()
            if (it.isFocused && host.activeFieldId != fieldId) host.request(fieldId, consumer, value)
        },
        enabled = enabled,
        style = style,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
    )
}
