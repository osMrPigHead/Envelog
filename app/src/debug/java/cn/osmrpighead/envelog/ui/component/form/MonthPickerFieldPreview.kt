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
import java.time.YearMonth

@LangPreview
@Composable
private fun MonthPickerFieldPreview() {
    EnvelogTheme(dynamicColor = false) {
        var yearMonth by remember { mutableStateOf(YearMonth.of(2026, 9)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            MonthPickerFieldSample("Default") {
                MonthPickerField(
                    yearMonth = yearMonth,
                    sheetBuilder = MonthPickerSheetBuilder.Default(
                        MonthPickerConfirmEventHandler.Default { yearMonth = it },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            MonthPickerFieldSample("Label + supporting text") {
                MonthPickerField(
                    yearMonth = yearMonth,
                    sheetBuilder = MonthPickerSheetBuilder.Default(
                        MonthPickerConfirmEventHandler.Default { yearMonth = it },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = "Month") },
                    supportingText = { Text(text = "Tap to pick a month") },
                )
            }
        }
    }
}

@LangPreview
@Composable
private fun MonthPickerFieldRangePreview() {
    EnvelogTheme(dynamicColor = false) {
        var constrained by remember { mutableStateOf(YearMonth.of(2026, 6)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            MonthPickerFieldSample("Range 2026-03..2026-09") {
                MonthPickerField(
                    yearMonth = constrained,
                    sheetBuilder = MonthPickerSheetBuilder.Default(
                        MonthPickerConfirmEventHandler.Default { constrained = it },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minYearMonth = YearMonth.of(2026, 3),
                    maxYearMonth = YearMonth.of(2026, 9),
                    label = { Text(text = "Budget month") },
                )
            }
            MonthPickerFieldSample("Disabled") {
                MonthPickerField(
                    yearMonth = YearMonth.of(2026, 9),
                    sheetBuilder = MonthPickerSheetBuilder.Default(MonthPickerConfirmEventHandler.Default {}),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun MonthPickerFieldSample(
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
