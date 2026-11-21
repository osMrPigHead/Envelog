package cn.osmrpighead.envelog.ui.component.form.textfield

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle

data class EnvelogOutlinedTextFieldStyle(
    val singleLine: Boolean = true,
    val maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    val minLines: Int = 1,
    val shape: Shape = ShapeDefaults.ExtraSmall,
    val colorsFunction: @Composable () -> TextFieldColors = { OutlinedTextFieldDefaults.colors() },
    val contentPadding: PaddingValues = OutlinedTextFieldDefaults.contentPadding(),
) {
    val colors @Composable get() = colorsFunction()
}

val LocalEnvelogOutlinedTextFieldStyle = staticCompositionLocalOf { EnvelogOutlinedTextFieldStyle() }

@Composable
fun ProvideOutlinedTextFieldStyle(
    value: EnvelogOutlinedTextFieldStyle,
    textStyle: TextStyle = LocalTextStyle.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalEnvelogOutlinedTextFieldStyle provides value,
        LocalTextStyle provides textStyle,
        content = content,
    )
}
