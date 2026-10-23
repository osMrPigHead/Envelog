package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadState

@Composable
fun NumKeypadField(
    value: String,
    host: NumKeypadHostState,
    consumer: NumKeypadConsumer,
    modifier: Modifier = Modifier,
    fieldId: Any = remember { Any() },
    enabled: Boolean = true,
    readOnly: Boolean = true,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    interactionSource: MutableInteractionSource? = null,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    val internalSource = remember { MutableInteractionSource() }
    val effectiveSource = interactionSource ?: internalSource

    val consumer by rememberUpdatedState(consumer)
    var isFocused by remember { mutableStateOf(false) }

    PickerField(
        value = if (host.activeFieldId == fieldId) host.state.text else value,
        onClick = {
            if (host.activeFieldId != fieldId) host.request(fieldId, consumer, value)
        },
        modifier = modifier.onFocusChanged {
            if (isFocused && !it.isFocused) host.safeClose()
            isFocused = it.isFocused
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        interactionSource = effectiveSource,
        shape = shape,
        colors = colors,
    )
}
