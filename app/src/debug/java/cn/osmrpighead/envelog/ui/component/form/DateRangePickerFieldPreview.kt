package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.Arrangement
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
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.untilInclusive
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.time.LocalDate

@LangPreview
@Composable
private fun DateRangePickerFieldPreview() {
    EnvelogTheme(dynamicColor = false) {
        var span by remember {
            mutableStateOf(LocalDate.of(2026, 9, 1) untilInclusive LocalDate.of(2026, 9, 30))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            DateRangePickerFieldSample("Default") {
                DateRangePickerField(
                    period = span,
                    dialogBuilder = DateRangePickerDialogBuilder.Default(ConfirmEventHandler.Default { span = it }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DateRangePickerFieldSample("Label + supporting text") {
                DateRangePickerField(
                    period = span,
                    dialogBuilder = DateRangePickerDialogBuilder.Default(ConfirmEventHandler.Default { span = it }),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Period") },
                    supportingText = { Text("Tap to pick a date range") },
                )
            }
        }
    }
}

@LangPreview
@Composable
private fun DateRangePickerFieldStatePreview() {
    EnvelogTheme(dynamicColor = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            DateRangePickerFieldSample("Error") {
                var span by remember {
                    mutableStateOf(
                        ClosedDateSpan(
                            startDate = LocalDate.of(2026, 10, 1),
                            endDate = LocalDate.of(2026, 9, 1),
                        ),
                    )
                }
                DateRangePickerField(
                    period = span,
                    dialogBuilder = DateRangePickerDialogBuilder.Default(ConfirmEventHandler.Default { span = it }),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("End date must be after the start date") },
                    isError = true,
                )
            }
            DateRangePickerFieldSample("Disabled") {
                DateRangePickerField(
                    period = LocalDate.of(2026, 9, 1) untilInclusive LocalDate.of(2026, 9, 30),
                    dialogBuilder = DateRangePickerDialogBuilder.Default(ConfirmEventHandler.Default {}),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun DateRangePickerFieldSample(
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
