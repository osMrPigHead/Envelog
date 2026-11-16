package cn.osmrpighead.envelog.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

object ButtonColor {
    val danger: ButtonColors
        @Composable get() = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
    val outlinedDanger: ButtonColors
        @Composable get() = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        )
    val textDanger: ButtonColors
        @Composable get() = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        )
    val textNormal: ButtonColors
        @Composable get() = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
}
