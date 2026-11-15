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
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.time.DayOfWeek

@LangPreview
@Composable
private fun DayOfWeekRangePickerFieldPreview() {
    EnvelogTheme(dynamicColor = false) {
        var startDayOfWeek by remember { mutableStateOf(DayOfWeek.FRIDAY) }
        var endDayOfWeek by remember { mutableStateOf(DayOfWeek.TUESDAY) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            DayOfWeekRangePickerFieldSample("Wrapped range (Fri ~ Tue)") {
                DayOfWeekRangePickerField(
                    startDayOfWeek = startDayOfWeek,
                    endDayOfWeek = endDayOfWeek,
                    sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                        DayOfWeekRangePickerConfirmEventHandler.Default { start, end ->
                            startDayOfWeek = start
                            endDayOfWeek = end
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DayOfWeekRangePickerFieldSample("Label + supporting text") {
                DayOfWeekRangePickerField(
                    startDayOfWeek = startDayOfWeek,
                    endDayOfWeek = endDayOfWeek,
                    sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                        DayOfWeekRangePickerConfirmEventHandler.Default { start, end ->
                            startDayOfWeek = start
                            endDayOfWeek = end
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Weekly period") },
                    supportingText = { Text(text = "Tap to pick a day range") },
                )
            }
        }
    }
}

@LangPreview
@Composable
private fun DayOfWeekRangePickerFieldStatePreview() {
    EnvelogTheme(dynamicColor = false) {
        var startDayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY) }
        var endDayOfWeek by remember { mutableStateOf(DayOfWeek.MONDAY) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            DayOfWeekRangePickerFieldSample("Same day") {
                DayOfWeekRangePickerField(
                    startDayOfWeek = startDayOfWeek,
                    endDayOfWeek = endDayOfWeek,
                    sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                        DayOfWeekRangePickerConfirmEventHandler.Default { start, end ->
                            startDayOfWeek = start
                            endDayOfWeek = end
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            DayOfWeekRangePickerFieldSample("Disabled") {
                DayOfWeekRangePickerField(
                    startDayOfWeek = DayOfWeek.FRIDAY,
                    endDayOfWeek = DayOfWeek.TUESDAY,
                    sheetBuilder = DayOfWeekRangePickerSheetBuilder.Default(
                        DayOfWeekRangePickerConfirmEventHandler.Default { _, _ -> },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekRangePickerFieldSample(
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
