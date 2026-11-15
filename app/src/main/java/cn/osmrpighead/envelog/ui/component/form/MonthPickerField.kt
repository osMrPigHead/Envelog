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
import cn.osmrpighead.envelog.ui.text.yearMonthText
import java.time.YearMonth

sealed interface MonthPickerConfirmEventHandler {
    fun onConfirm(yearMonth: YearMonth, hideSheet: () -> Unit)

    class Custom(
        private val handler: (YearMonth, hideSheet: () -> Unit) -> Unit,
    ) : MonthPickerConfirmEventHandler {
        override fun onConfirm(yearMonth: YearMonth, hideSheet: () -> Unit) = handler(yearMonth, hideSheet)
    }

    class Default(
        private val onYearMonthChange: (YearMonth) -> Unit,
    ) : MonthPickerConfirmEventHandler {
        override fun onConfirm(yearMonth: YearMonth, hideSheet: () -> Unit) {
            onYearMonthChange(yearMonth)
            hideSheet()
        }
    }
}

@Composable
fun MonthPickerField(
    yearMonth: YearMonth,
    sheetBuilder: MonthPickerSheetBuilder,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = true,
    minYearMonth: YearMonth? = null,
    maxYearMonth: YearMonth? = null,
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
    require(minYearMonth == null || maxYearMonth == null || minYearMonth <= maxYearMonth) {
        "minYearMonth must not be after maxYearMonth"
    }

    val internalSource = remember { MutableInteractionSource() }
    val effectiveSource = interactionSource ?: internalSource
    var showSheet by remember { mutableStateOf(false) }

    PickerField(
        value = yearMonthText(yearMonth),
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
        yearMonth = yearMonth,
        minYearMonth = minYearMonth,
        maxYearMonth = maxYearMonth,
        hideSheet = { showSheet = false },
    )
}
