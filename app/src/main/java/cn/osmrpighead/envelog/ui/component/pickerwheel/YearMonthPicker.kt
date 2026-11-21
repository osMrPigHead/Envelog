package cn.osmrpighead.envelog.ui.component.pickerwheel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.util.hideAndThen
import kotlinx.coroutines.launch
import java.time.YearMonth

@Immutable
data class MonthPickerColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val contentColor: Color,
)

object MonthPickerDefaults {
    @Composable
    fun colors(
        selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ) = MonthPickerColors(
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        contentColor = contentColor,
    )
}

@Composable
fun YearMonthPicker(
    yearMonth: YearMonth,
    minYearMonth: YearMonth?,
    maxYearMonth: YearMonth?,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)?,
    title: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    colors: MonthPickerColors = MonthPickerDefaults.colors(),
) {
    val range = remember(minYearMonth, maxYearMonth) { MonthPickerRange(minYearMonth, maxYearMonth) }
    val initialSelection = remember(range, yearMonth) { range.coerce(yearMonth) }
    var selected by remember(initialSelection) { mutableStateOf(initialSelection) }
    val years = remember(range, initialSelection) {
        range.effectiveYearRange(initialSelection).toList()
    }
    val months = remember(range, selected.year) { range.monthsIn(selected.year) }
    val yearIndex = years.indexOf(selected.year).coerceAtLeast(0)
    val monthIndex = months.indexOf(selected.monthValue).coerceAtLeast(0)
    val wheelHeight = WheelItemHeight * WheelVisibleItemCount.toFloat()

    fun selectYear(year: Int) {
        val availableMonths = range.monthsIn(year)
        if (availableMonths.isEmpty()) return
        selected = YearMonth.of(
            year,
            selected.monthValue.coerceIn(availableMonths.first(), availableMonths.last()),
        )
    }

    fun selectMonth(month: Int) {
        selected = YearMonth.of(selected.year, month)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimensionResource(R.dimen.spacing8))
            .padding(bottom = dimensionResource(R.dimen.spacing8)),
    ) {
        title?.invoke()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacing3)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing6)),
        ) {
            WheelLabel(
                text = stringResource(R.string.month_picker_label_year),
                modifier = Modifier.weight(1f),
            )
            WheelLabel(
                text = stringResource(R.string.month_picker_label_month),
                modifier = Modifier.weight(1f),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacing1))
                .height(wheelHeight),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(WheelItemHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .background(colors.selectedContainerColor),
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing6)),
            ) {
                PickerWheel(
                    items = years,
                    selectedIndex = yearIndex,
                    onSelectedIndexChange = { index -> selectYear(years[index]) },
                    modifier = Modifier.weight(1f),
                ) { year, isSelected ->
                    WheelItem(
                        text = year.toString(),
                        selected = isSelected,
                        selectedContentColor = colors.selectedContentColor,
                        contentColor = colors.contentColor,
                    )
                }
                PickerWheel(
                    items = months,
                    selectedIndex = monthIndex,
                    onSelectedIndexChange = { index -> selectMonth(months[index]) },
                    modifier = Modifier.weight(1f),
                ) { month, isSelected ->
                    WheelItem(
                        text = month.toString().padStart(2, '0'),
                        selected = isSelected,
                        selectedContentColor = colors.selectedContentColor,
                        contentColor = colors.contentColor,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacing8)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismissButton?.invoke(onDismiss)
            Button(
                onClick = { onConfirm(selected) },
                enabled = range.contains(selected),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing4)),
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearMonthPickerModalBottomSheet(
    yearMonth: YearMonth,
    onHide: () -> Unit,
    onConfirm: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    minYearMonth: YearMonth? = null,
    maxYearMonth: YearMonth? = null,
) {
    val scope = rememberCoroutineScope()
    val hideSheetAnimated: () -> Unit = {
        scope.launch { sheetState.hideAndThen { onHide() } }
    }

    LaunchedEffect(sheetState) { sheetState.show() }

    ModalBottomSheet(
        onDismissRequest = hideSheetAnimated,
        sheetState = sheetState,
        sheetGesturesEnabled = false,
        modifier = modifier,
    ) {
        YearMonthPicker(
            yearMonth = yearMonth,
            minYearMonth = minYearMonth,
            maxYearMonth = maxYearMonth,
            onConfirm = {
                onConfirm(it)
                hideSheetAnimated()
            },
            onDismiss = hideSheetAnimated,
            dismissButton = { TextButton(onClick = it) { Text(stringResource(R.string.common_action_cancel)) } },
            title = {
                Text(
                    text = stringResource(R.string.month_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        )
    }
}
