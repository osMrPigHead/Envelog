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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.time.DayOfWeek

sealed interface DayOfWeekRangePickerConfirmEventHandler {
    fun onConfirm(startDayOfWeek: DayOfWeek, endDayOfWeek: DayOfWeek, hideSheet: () -> Unit)

    class Custom(
        private val handler: (DayOfWeek, DayOfWeek, hideSheet: () -> Unit) -> Unit,
    ) : DayOfWeekRangePickerConfirmEventHandler {
        override fun onConfirm(startDayOfWeek: DayOfWeek, endDayOfWeek: DayOfWeek, hideSheet: () -> Unit) =
            handler(startDayOfWeek, endDayOfWeek, hideSheet)
    }

    class Default(
        private val onDayOfWeekRangeChange: (DayOfWeek, DayOfWeek) -> Unit,
    ) : DayOfWeekRangePickerConfirmEventHandler {
        override fun onConfirm(startDayOfWeek: DayOfWeek, endDayOfWeek: DayOfWeek, hideSheet: () -> Unit) {
            onDayOfWeekRangeChange(startDayOfWeek, endDayOfWeek)
            hideSheet()
        }
    }
}

@Composable
fun DayOfWeekRangePickerField(
    startDayOfWeek: DayOfWeek,
    endDayOfWeek: DayOfWeek,
    sheetBuilder: DayOfWeekRangePickerSheetBuilder,
    modifier: Modifier = Modifier,
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
    var showSheet by remember { mutableStateOf(false) }

    PickerField(
        value = dayOfWeekRangeText(startDayOfWeek, endDayOfWeek),
        onClick = { showSheet = true },
        modifier = modifier,
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

    if (showSheet) sheetBuilder.Composable(
        startDayOfWeek = startDayOfWeek,
        endDayOfWeek = endDayOfWeek,
        hideSheet = { showSheet = false },
    )
}
