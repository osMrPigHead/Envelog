package cn.osmrpighead.envelog.ui.component.form.textfield

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R

object EnvelogTextFieldDefaults {
    val NormalTextStyle @Composable get() = MaterialTheme.typography.bodyLarge
    val Normal = EnvelogTextFieldStyle()

    val SubtleTextStyle @Composable get() = MaterialTheme.typography.bodyMedium
    val Subtle
        @Composable get() = EnvelogTextFieldStyle(
            colorsFunction = {
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.outline,
                )
            },
            contentPaddingWithoutLabel = TextFieldDefaults.contentPaddingWithoutLabel(
                top = dimensionResource(R.dimen.subtleFieldVerticalPadding),
                bottom = dimensionResource(R.dimen.subtleFieldVerticalPadding),
            ),
        )

    @Composable
    fun ProvideNormal(content: @Composable () -> Unit) = ProvideTextFieldStyle(
        value = Normal,
        textStyle = NormalTextStyle,
        content = content,
    )

    @Composable
    fun ProvideSubtle(content: @Composable () -> Unit) = ProvideTextFieldStyle(
        value = Subtle,
        textStyle = SubtleTextStyle,
        content = content,
    )

    // TODO: Unselected
}