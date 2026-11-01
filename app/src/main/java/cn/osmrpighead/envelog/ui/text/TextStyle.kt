package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

val TextStyle.lineHeightAsDp: Dp
    @Composable get() = with(LocalDensity.current) { lineHeight.toDp() }
