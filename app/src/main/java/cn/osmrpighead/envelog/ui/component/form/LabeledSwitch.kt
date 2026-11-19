package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import cn.osmrpighead.envelog.R

@Composable
fun LabeledSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    label: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(
                horizontal = dimensionResource(R.dimen.spacing8),
                vertical = dimensionResource(R.dimen.spacing2),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(1f).padding(end = dimensionResource(R.dimen.spacing4))) { label() }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
