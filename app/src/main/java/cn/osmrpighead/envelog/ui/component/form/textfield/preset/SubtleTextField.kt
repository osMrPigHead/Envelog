package cn.osmrpighead.envelog.ui.component.form.textfield.preset

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogTextField
import cn.osmrpighead.envelog.ui.component.form.textfield.EnvelogTextFieldDefaults
import cn.osmrpighead.envelog.ui.style.subtleField

@Composable
fun SubtleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
) {
    EnvelogTextFieldDefaults.ProvideSubtle {
        EnvelogTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = modifier.subtleField(),
        )
    }
}
