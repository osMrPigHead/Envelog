package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R

@Composable
fun BudgetOverlapsErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.common_label_error))
        },
        text = {
            Text(stringResource(R.string.envelope_edit_msg_budget_overlaps))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        },
    )
}
