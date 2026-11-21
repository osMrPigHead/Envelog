package cn.osmrpighead.envelog.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DarkColorScheme = darkColorScheme()
val LightColorScheme = lightColorScheme()

@Immutable
data class EnvelogColorScheme(
    val onSurfaceTertiary: Color,
    val positive: Color,
    val health: Color,
)

val EnvelogDarkColorScheme = EnvelogColorScheme(
    onSurfaceTertiary = Color(red = 147, green = 143, blue = 153),
    positive = Color(0xff9dcafc),
    health = Color(0xff9dcafc),
)
val EnvelogLightColorScheme = EnvelogColorScheme(
    onSurfaceTertiary = Color(red = 121, green = 116, blue = 126),
    positive = Color(0xff32618d),
    health = Color(0xff32618d),
)

val LocalEnvelogColorScheme = staticCompositionLocalOf { EnvelogLightColorScheme }
