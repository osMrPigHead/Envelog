package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun PickerFieldPreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            PickerFieldSample("Default") {
                PickerField(
                    value = "2026/9/1 ~ 2026/9/30",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            PickerFieldSample("Label + supporting text") {
                PickerField(
                    value = "2026/9/1 ~ 2026/9/30",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Period") },
                    supportingText = { Text("Tap to pick a date range") },
                )
            }
            PickerFieldSample("Placeholder") {
                PickerField(
                    value = "",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Period") },
                    placeholder = { Text("Select a date range") },
                )
            }
        }
    }
}

@LangPreview
@Composable
private fun PickerFieldStatePreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            PickerFieldSample("Error") {
                PickerField(
                    value = "2026/9/31 ~ 2026/10/1",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("End date must be after the start date") },
                    isError = true,
                )
            }
            PickerFieldSample("Disabled") {
                PickerField(
                    value = "2026/9/1 ~ 2026/9/30",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun PickerFieldSample(
    name: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingThin))) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
        content()
    }
}
