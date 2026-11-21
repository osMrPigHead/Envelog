package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.pickerwheel.YearMonthPickerModalBottomSheet
import cn.osmrpighead.envelog.ui.icon.Icons
import cn.osmrpighead.envelog.ui.text.yearMonthText
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutlinedYearMonthButton(
    yearMonth: YearMonth,
    onYearMonthChange: (YearMonth) -> Unit,
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
            imageVector = Icons.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing2)),
        )
    }

    if (showSheet) YearMonthPickerModalBottomSheet(
        yearMonth = yearMonth,
        minYearMonth = minYearMonth,
        maxYearMonth = maxYearMonth,
        onHide = { showSheet = false },
        onConfirm = onYearMonthChange,
    )
}
