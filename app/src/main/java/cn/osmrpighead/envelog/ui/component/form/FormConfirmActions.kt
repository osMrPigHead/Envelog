package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R

@Composable
fun FormConfirmActions(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    cancel: @Composable RowScope.() -> Unit = { Text(text = stringResource(R.string.common_action_cancel)) },
    confirm: @Composable RowScope.() -> Unit = { Text(text = stringResource(R.string.common_action_ok)) },
    before: @Composable RowScope.() -> Unit = {},
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space8), Alignment.End),
        modifier = modifier,
    ) {
        before()
        TextButton(
            onClick = onCancel,
            enabled = cancelEnabled,
            content = cancel,
        )
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            content = confirm,
        )
    }
}
