package cn.osmrpighead.envelog.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Set of Material typography styles to start with
val Typography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 26.0.sp,
        fontSize = 19.sp,
        letterSpacing = 0.0.sp,
        fontWeight = FontWeight.Medium,
    ),
)

data class EnvelogTypography(
    private val typography: Typography,
) {
    val fieldMedium = typography.bodyMedium.copy(lineHeight = typography.bodyLarge.lineHeight)
    val labelExtra = typography.bodyLarge.copy(fontWeight = typography.labelLarge.fontWeight)
}

val LocalEnvelogTypography = staticCompositionLocalOf { EnvelogTypography(Typography) }
