package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePickerColors
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerFormatter
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.util.date.ClosedDatePeriod
import cn.osmrpighead.envelog.util.date.toLocalDate
import java.time.LocalDate

sealed interface ConfirmEventHandler {
    fun onConfirm(start: LocalDate?, end: LocalDate?, hideDialog: () -> Unit)

    class Custom(private val handler: (LocalDate?, LocalDate?, hideDialog: () -> Unit) -> Unit) : ConfirmEventHandler {
        override fun onConfirm(start: LocalDate?, end: LocalDate?, hideDialog: () -> Unit) =
            handler(start, end, hideDialog)
    }

    class Default(private val onPeriodChange: (ClosedDatePeriod) -> Unit) : ConfirmEventHandler {
        override fun onConfirm(start: LocalDate?, end: LocalDate?, hideDialog: () -> Unit) {
            if (start == null || end == null) return
            onPeriodChange(ClosedDatePeriod(start, end))
            hideDialog()
        }
    }
}

sealed interface DateRangePickerDialogBuilder {
    @Composable
    fun Composable(hideDialog: () -> Unit, modifier: Modifier = Modifier)

    class Custom(private val composable: @Composable (hideDialog: () -> Unit) -> Unit) : DateRangePickerDialogBuilder {
        @Composable
        override fun Composable(hideDialog: () -> Unit, modifier: Modifier) = composable(hideDialog)
    }

    class Default(
        private val confirmEventHandler: ConfirmEventHandler,
        private val onDismiss: (hideDialog: () -> Unit) -> Unit = { it() },
        private val dismissButton: (@Composable (hideDialog: () -> Unit) -> Unit)? = {
            TextButton(onClick = it) { Text(stringResource(R.string.cancel)) }
        },
        private val dateFormatter: DatePickerFormatter? = null,
        private val colors: DatePickerColors? = null,
        private val title: (@Composable () -> Unit)? = null,
        private val headline: (@Composable () -> Unit)? = null,
    ) : DateRangePickerDialogBuilder {
        @Composable
        override fun Composable(hideDialog: () -> Unit, modifier: Modifier) {
            val dateRangePickerState = rememberDateRangePickerState()
            DatePickerDialog(
                onDismissRequest = { onDismiss(hideDialog) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmEventHandler.onConfirm(
                                dateRangePickerState.selectedStartDateMillis?.toLocalDate(),
                                dateRangePickerState.selectedEndDateMillis?.toLocalDate(),
                                hideDialog,
                            )
                        },
                    ) { Text(stringResource(R.string.ok)) }
                },
                modifier = modifier,
                dismissButton = dismissButton?.let { { it(hideDialog) } },
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    dateFormatter = dateFormatter ?: remember { DatePickerDefaults.dateFormatter() },
                    colors = colors ?: DatePickerDefaults.colors(),
                    title = title,
                    headline = headline,
                )
            }
        }
    }
}

@Composable
fun DateRangePickerField(
    period: ClosedDatePeriod,
    dialogBuilder: DateRangePickerDialogBuilder,
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

    var showPicker by remember { mutableStateOf(false) }

    PickerField(
        value = period.format(),
        onClick = { showPicker = true },
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

    if (showPicker) dialogBuilder.Composable({ showPicker = false })
}
