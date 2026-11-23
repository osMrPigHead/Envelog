package cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.LocalEnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.pickerwheel.DayOfWeekRangePickerModalBottomSheet
import cn.osmrpighead.envelog.ui.text.dayOfWeekRangeText
import java.time.DayOfWeek

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedDayOfWeekRangeField(
    startDayOfWeek: DayOfWeek,
    endDayOfWeek: DayOfWeek,
    onDayOfWeekRangeChange: (DayOfWeek, DayOfWeek) -> Unit,
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
    onOpen: () -> Unit = {},
) {
    var showPicker by remember { mutableStateOf(false) }

    OutlinedInteractionField(
        value = dayOfWeekRangeText(startDayOfWeek, endDayOfWeek),
        onClick = {
            onOpen()
            showPicker = true
        },
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

    if (showPicker) DayOfWeekRangePickerModalBottomSheet(
        startDayOfWeek = startDayOfWeek,
        endDayOfWeek = endDayOfWeek,
        onHide = { showPicker = false },
        onConfirm = onDayOfWeekRangeChange,
    )
}
