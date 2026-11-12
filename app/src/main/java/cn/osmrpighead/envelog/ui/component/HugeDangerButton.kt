package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.theme.ButtonColor

@Composable
fun HugeDangerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonColor.danger,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacingExtra)),
        content = content
    )
}
