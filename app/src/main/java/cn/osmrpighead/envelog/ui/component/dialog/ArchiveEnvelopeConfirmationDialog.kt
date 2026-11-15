package cn.osmrpighead.envelog.ui.component.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R

@Composable
fun ArchiveEnvelopeConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
            onDismissRequest = onDismiss,
    title = {
        Text(text = stringResource(R.string.common_action_archive_envelope))
    },
    text = {
        Text(text = stringResource(R.string.common_msg_archive_envelope_confirm))
    },
    dismissButton = {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.common_action_cancel))
        }
    },
    confirmButton = {
        TextButton(
            onClick = onConfirm,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.common_action_archive))
        }
    },
    )
}
