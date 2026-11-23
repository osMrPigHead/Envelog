package cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextField
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.LocalEnvelogOutlinedTextFieldStyle

@Composable
fun OutlinedInteractionField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
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
    val source = remember { MutableInteractionSource() }
    val onClick by rememberUpdatedState(onClick)
    LaunchedEffect(source) {
        source.interactions.collect { interaction -> if (interaction is PressInteraction.Release) onClick() }
    }

    EnvelogOutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = modifier,
        enabled = enabled,
        readOnly = true,
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
        interactionSource = source,
    )
}
