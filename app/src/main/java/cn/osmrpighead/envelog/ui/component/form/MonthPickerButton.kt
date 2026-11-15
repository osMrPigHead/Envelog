package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.text.yearMonthText
import java.time.YearMonth

@Composable
fun MonthPickerButton(
    yearMonth: YearMonth,
    sheetBuilder: MonthPickerSheetBuilder,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minYearMonth: YearMonth? = null,
    maxYearMonth: YearMonth? = null,
) {
    require(minYearMonth == null || maxYearMonth == null || minYearMonth <= maxYearMonth) {
        "minYearMonth must not be after maxYearMonth"
    }

    var showSheet by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showSheet = true },
        modifier = modifier,
        enabled = enabled,
        shape = CircleShape,
    ) {
        Text(text = yearMonthText(yearMonth))
        Icon(
            imageVector = Icon.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingThin)),
        )
    }

    if (showSheet) sheetBuilder.Composable(
        yearMonth = yearMonth,
        minYearMonth = minYearMonth,
        maxYearMonth = maxYearMonth,
        hideSheet = { showSheet = false },
    )
}
