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
private fun MonthPickerButtonPreview() {
    EnvelogTheme(dynamicColor = false) {
        var yearMonth by remember { mutableStateOf(YearMonth.of(2026, 9)) }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            MonthPickerButtonSample("Default") {
                MonthPickerButton(
                    yearMonth = yearMonth,
                    sheetBuilder = MonthPickerSheetBuilder.Default(
                        MonthPickerConfirmEventHandler.Default { yearMonth = it },
                    ),
                )
            }
            MonthPickerButtonSample("Range 2026-03..2026-09") {
                MonthPickerButton(
                    yearMonth = yearMonth,
                    sheetBuilder = MonthPickerSheetBuilder.Default(
                        MonthPickerConfirmEventHandler.Default { yearMonth = it },
                    ),
                    minYearMonth = YearMonth.of(2026, 3),
                    maxYearMonth = YearMonth.of(2026, 9),
                )
            }
            MonthPickerButtonSample("Disabled") {
                MonthPickerButton(
                    yearMonth = yearMonth,
                    sheetBuilder = MonthPickerSheetBuilder.Default(MonthPickerConfirmEventHandler.Default {}),
                    enabled = false,
                )
            }
        }
    }
}

@Composable
private fun MonthPickerButtonSample(
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
