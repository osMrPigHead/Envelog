package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeSubmissionError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeSubmissionErrorDialog(
    error: EnvelopeSubmissionError,
    onDismiss: () -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = dimensionResource(R.dimen.elevationMedium),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacingExtra)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(
                    text = stringResource(R.string.common_label_error),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = when (error) {
                        EnvelopeSubmissionError.BudgetOverlaps -> stringResource(R.string.envelope_edit_msg_budget_overlaps)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_action_ok)) }
                }
            }
        }
    }
}
