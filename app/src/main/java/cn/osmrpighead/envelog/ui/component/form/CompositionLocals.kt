package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalFormEnabled = staticCompositionLocalOf { true }
val LocalFormActive = staticCompositionLocalOf { true }

// TODO: Styles?
@Composable
fun ProvideForm(
    enabled: Boolean = LocalFormEnabled.current,
    active: Boolean = LocalFormActive.current,
    content: @Composable () -> Unit,
) =
    CompositionLocalProvider(
        LocalFormEnabled provides enabled,
        LocalFormActive provides active,
        content = content,
    )
