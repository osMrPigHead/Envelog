package cn.osmrpighead.envelog.ui.util

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle

@Composable
fun (@Composable () -> Unit).withTextStyle(value: TextStyle): @Composable () -> Unit =
    this.let { { ProvideTextStyle(value, it) } }
