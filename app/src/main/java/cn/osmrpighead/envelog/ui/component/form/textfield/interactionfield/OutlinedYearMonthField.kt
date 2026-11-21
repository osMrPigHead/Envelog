package cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.form.textfield.LocalEnvelogOutlinedTextFieldStyle
import cn.osmrpighead.envelog.ui.component.pickerwheel.YearMonthPickerModalBottomSheet
import cn.osmrpighead.envelog.ui.text.yearMonthText
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedYearMonthField(
    yearMonth: YearMonth,
    onYearMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minYearMonth: YearMonth? = null,
    maxYearMonth: YearMonth? = null,
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
    require(minYearMonth == null || maxYearMonth == null || minYearMonth <= maxYearMonth) {
        "minYearMonth must not be after maxYearMonth"
    }

    var showPicker by remember { mutableStateOf(false) }

    OutlinedInteractionField(
        value = yearMonthText(yearMonth),
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

    if (showPicker) YearMonthPickerModalBottomSheet(
        yearMonth = yearMonth,
        minYearMonth = minYearMonth,
        maxYearMonth = maxYearMonth,
        onHide = { showPicker = false },
        onConfirm = onYearMonthChange,
    )
}
