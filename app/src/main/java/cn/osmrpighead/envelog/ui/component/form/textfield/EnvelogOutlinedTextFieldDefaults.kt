package cn.osmrpighead.envelog.ui.component.form.textfield

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

object EnvelogOutlinedTextFieldDefaults {
    val NormalTextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val Normal = EnvelogOutlinedTextFieldStyle()

    @Composable
    fun ProvideNormal(content: @Composable () -> Unit) = ProvideOutlinedTextFieldStyle(
        value = Normal,
        textStyle = NormalTextStyle,
        content = content,
    )

    // TODO: Subtle

    val Unselected = EnvelogOutlinedTextFieldStyle(
        colorsFunction = {
            OutlinedTextFieldDefaults.colors(
                disabledTextColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
                disabledLabelColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledPlaceholderColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledLeadingIconColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledTrailingIconColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledSupportingTextColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledPrefixColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
                disabledSuffixColor = EnvelogTheme.colorScheme.onSurfaceTertiary,
            )
        },
    )

    @Composable
    fun ProvideUnselected(content: @Composable () -> Unit) = ProvideOutlinedTextFieldStyle(
        value = Unselected,
        content = content,
    )
}