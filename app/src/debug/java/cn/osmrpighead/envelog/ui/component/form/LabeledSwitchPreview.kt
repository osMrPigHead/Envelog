package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun LabeledSwitchPreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(modifier = Modifier.fillMaxWidth()) {
            var budgeted by remember { mutableStateOf(true) }
            LabeledSwitchSample("On") {
                LabeledSwitch(
                    checked = budgeted,
                    onCheckedChange = { budgeted = it },
                ) {
                    Text(
                        text = stringResource(R.string.envelope_budgeted),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            var notifyOverspent by remember { mutableStateOf(false) }
            LabeledSwitchSample("Off") {
                LabeledSwitch(
                    checked = notifyOverspent,
                    onCheckedChange = { notifyOverspent = it },
                ) {
                    Text(
                        text = "Notify when overspent",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            LabeledSwitchSample("Two-line label") {
                LabeledSwitch(
                    checked = true,
                    onCheckedChange = {},
                ) {
                    Column {
                        Text(
                            text = "Carry over leftover",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Move the remaining amount to the next period",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            LabeledSwitchSample("Long label") {
                LabeledSwitch(
                    checked = true,
                    onCheckedChange = {},
                ) {
                    Text(
                        text = "Notify when the total spending of an envelope exceeds its budget",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@LangPreview
@Composable
private fun LabeledSwitchStatePreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LabeledSwitchSample("Disabled (on)") {
                LabeledSwitch(
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                ) {
                    Text(
                        text = stringResource(R.string.envelope_budgeted),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            LabeledSwitchSample("Disabled (off)") {
                LabeledSwitch(
                    checked = false,
                    onCheckedChange = {},
                    enabled = false,
                ) {
                    Text(
                        text = "Notify when overspent",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledSwitchSample(
    name: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(
                start = dimensionResource(R.dimen.spacingBold),
                top = dimensionResource(R.dimen.spacingLarge),
            ),
        )
        content()
    }
}
