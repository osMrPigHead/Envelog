package cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.core.time.toUtcMillis
import cn.osmrpighead.envelog.core.time.untilInclusive
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.LocalEnvelogOutlinedTextFieldStyle

@Composable
fun OutlinedDateRangeField(
    span: ClosedDateSpan,
    onSpanChange: (ClosedDateSpan) -> Unit,
    modifier: Modifier = Modifier,
    selectableDates: SelectableDates = DatePickerDefaults.AllDates,
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
    var showPicker by remember { mutableStateOf(false) }

    OutlinedInteractionField(
        value = span.format(),
        onClick = { showPicker = true },
        modifier = modifier,
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

    if (showPicker) {
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = span.startDate.toUtcMillis(),
            initialSelectedEndDateMillis = span.endDate.toUtcMillis(),
            selectableDates = selectableDates,
        )
        val hide = { showPicker = false }
        DatePickerDialog(
            onDismissRequest = hide,
            dismissButton = { TextButton(onClick = hide) { Text(stringResource(R.string.common_action_cancel)) } },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPicker = false
                        val start = state.selectedStartDateMillis?.toLocalDate() ?: return@TextButton hide()
                        val end = state.selectedEndDateMillis?.toLocalDate() ?: start
                        onSpanChange(start untilInclusive end)
                    },
                ) { Text(stringResource(R.string.common_action_ok)) }
            },
        ) {
            DateRangePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                modifier = Modifier.padding(vertical = dimensionResource(R.dimen.space16))
            )
        }
    }
}
