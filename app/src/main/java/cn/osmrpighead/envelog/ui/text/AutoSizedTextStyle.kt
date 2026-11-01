package cn.osmrpighead.envelog.ui.text

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.TextAutoSizeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

data class AutoSizedTextStyle(
    val textStyle: TextStyle,
    val autoSize: TextAutoSize,
) {
    constructor(textStyle: TextStyle) : this(
        textStyle = textStyle,
        autoSize = TextAutoSize.StepBased(
            minFontSize = TextAutoSizeDefaults.MinFontSize,
            maxFontSize = textStyle.fontSize,
        )
    )
}

val AutoSizedTextStyle.lineHeightAsDp: Dp
    @Composable get() = textStyle.lineHeightAsDp
