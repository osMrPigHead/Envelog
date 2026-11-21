package cn.osmrpighead.envelog.ui.component.form.textfield

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

data class EnvelogTextFieldStyle(
    val singleLine: Boolean = true,
    val maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    val minLines: Int = 1,
    val shape: Shape = ShapeDefaults.ExtraSmall.copy(bottomStart = ZeroCornerSize, bottomEnd = ZeroCornerSize),
    val colorsFunction: @Composable () -> TextFieldColors = { TextFieldDefaults.colors() },
    val contentPaddingWithLabel: PaddingValues = TextFieldDefaults.contentPaddingWithLabel(),
    val contentPaddingWithoutLabel: PaddingValues = TextFieldDefaults.contentPaddingWithoutLabel(),
) {
    val colors @Composable get() = colorsFunction()
}

val LocalEnvelogTextFieldStyle = staticCompositionLocalOf { EnvelogTextFieldStyle() }

@Composable
fun ProvideTextFieldStyle(
    value: EnvelogTextFieldStyle,
    textStyle: TextStyle = LocalTextStyle.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalEnvelogTextFieldStyle provides value,
        LocalTextStyle provides textStyle,
        content = content,
    )
}
