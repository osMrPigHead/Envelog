package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
fun String?.orDefault(default: Int): String = this ?: stringResource(default)
