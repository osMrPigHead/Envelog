package cn.osmrpighead.envelog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

object EnvelogTheme {
    val darkTheme: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkTheme.current
    val colorScheme: EnvelogColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalEnvelogColorScheme.current
    val typography: EnvelogTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalEnvelogTypography.current
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun EnvelogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        },
        shapes = Shapes,
        typography = Typography,
    ) {
        CompositionLocalProvider(
            LocalEnvelogColorScheme provides if (darkTheme) EnvelogDarkColorScheme else EnvelogLightColorScheme,
            LocalEnvelogTypography provides EnvelogTypography(MaterialTheme.typography),
            LocalDarkTheme provides darkTheme,
            content = content,
        )
    }
}