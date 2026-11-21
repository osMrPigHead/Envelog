package cn.osmrpighead.envelog.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext

object EnvelogTheme {
    val colorScheme: EnvelogColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalEnvelogColorScheme.current
}

@Composable
fun EnvelogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalEnvelogColorScheme provides if (darkTheme) EnvelogDarkColorScheme else EnvelogLightColorScheme
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
            content = content,
        )
    }
}