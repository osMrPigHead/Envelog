package cn.osmrpighead.envelog.ui.component.form.textfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import cn.osmrpighead.envelog.ui.component.form.LocalFormActive
import cn.osmrpighead.envelog.ui.component.form.LocalFormEnabled
import cn.osmrpighead.envelog.ui.util.withTextStyle

@Composable
fun EnvelogOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    localEnabled: Boolean = LocalFormEnabled.current,
    active: Boolean = LocalFormActive.current,
    readOnly: Boolean = false,
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val enabled = enabled && localEnabled && active
    val supportingText = if (active) supportingText else null
    val isError = active && isError
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            style.colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides style.colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .then(
                        if (label != null) {
                            Modifier
                                .semantics(mergeDescendants = true) {}
                                .padding(top = minimizedLabelHalfHeight())
                        } else {
                            Modifier
                        }
                    )
                    .defaultErrorSemantics(isError, "") // TODO: Whats that
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(style.colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = style.singleLine,
            maxLines = style.maxLines,
            minLines = style.minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder?.let { { ProvideTextStyle(textStyle, placeholder) } },
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix?.withTextStyle(MaterialTheme.typography.bodySmall),
                        suffix = suffix?.withTextStyle(MaterialTheme.typography.bodySmall),
                        supportingText = supportingText,
                        singleLine = style.singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = style.colors,
                        contentPadding = style.contentPadding,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = style.colors,
                                shape = style.shape,
                            )
                        },
                    )
                },
        )
    }
}

@Composable
fun EnvelogOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    localEnabled: Boolean = LocalFormEnabled.current,
    active: Boolean = LocalFormActive.current,
    readOnly: Boolean = false,
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
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val enabled = enabled && localEnabled && active
    val supportingText = if (active) supportingText else null
    val isError = active && isError
    val interactionSource = interactionSource ?: remember { MutableInteractionSource() }
    val textColor =
        textStyle.color.takeOrElse {
            val focused = interactionSource.collectIsFocusedAsState().value
            style.colors.textColor(enabled, isError, focused)
        }
    val mergedTextStyle = textStyle.merge(TextStyle(color = textColor))

    CompositionLocalProvider(LocalTextSelectionColors provides style.colors.textSelectionColors) {
        BasicTextField(
            value = value,
            modifier =
                modifier
                    .then(
                        if (label != null) {
                            Modifier
                                .semantics(mergeDescendants = true) {}
                                .padding(top = minimizedLabelHalfHeight())
                        } else {
                            Modifier
                        }
                    )
                    .defaultErrorSemantics(isError, "")
                    .defaultMinSize(
                        minWidth = OutlinedTextFieldDefaults.MinWidth,
                        minHeight = OutlinedTextFieldDefaults.MinHeight,
                    ),
            onValueChange = onValueChange,
            enabled = enabled,
            readOnly = readOnly,
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(style.colors.cursorColor(isError)),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = style.singleLine,
            maxLines = style.maxLines,
            minLines = style.minLines,
            decorationBox =
                @Composable { innerTextField ->
                    OutlinedTextFieldDefaults.DecorationBox(
                        value = value.text,
                        visualTransformation = visualTransformation,
                        innerTextField = innerTextField,
                        placeholder = placeholder,
                        label = label,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        prefix = prefix?.withTextStyle(MaterialTheme.typography.bodySmall),
                        suffix = suffix?.withTextStyle(MaterialTheme.typography.bodySmall),
                        supportingText = supportingText,
                        singleLine = style.singleLine,
                        enabled = enabled,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors = style.colors,
                        contentPadding = style.contentPadding,
                        container = {
                            OutlinedTextFieldDefaults.Container(
                                enabled = enabled,
                                isError = isError,
                                interactionSource = interactionSource,
                                colors = style.colors,
                                shape = style.shape,
                            )
                        },
                    )
                },
        )
    }
}
