package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    if (EnvelogTheme.darkTheme) FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
    else Button(
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
}
