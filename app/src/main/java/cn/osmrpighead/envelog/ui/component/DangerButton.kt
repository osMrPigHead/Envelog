package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.theme.ButtonColor

@Composable
fun DangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonColor.outlinedDanger,
        modifier = modifier,
        content = content
    )
}
