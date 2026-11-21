package cn.osmrpighead.envelog.ui.component.pickerwheel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.text.dayOfWeekText
import cn.osmrpighead.envelog.ui.util.hideAndThen
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@Immutable
data class DayOfWeekRangePickerColors(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val contentColor: Color,
)

object DayOfWeekRangePickerDefaults {
    @Composable
    fun colors(
        selectedContainerColor: Color = MaterialTheme.colorScheme.primaryContainer,
        selectedContentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    ) = DayOfWeekRangePickerColors(
        selectedContainerColor = selectedContainerColor,
        selectedContentColor = selectedContentColor,
        contentColor = contentColor,
    )
}

@Composable
fun DayOfWeekRangePicker(
    startDayOfWeek: DayOfWeek,
    endDayOfWeek: DayOfWeek,
    onConfirm: (DayOfWeek, DayOfWeek) -> Unit,
    onDismiss: () -> Unit,
    dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)?,
    title: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
    colors: DayOfWeekRangePickerColors = DayOfWeekRangePickerDefaults.colors(),
) {
    val days = DayOfWeek.entries
    var selectedStart by remember(startDayOfWeek) { mutableStateOf(startDayOfWeek) }
    var selectedEnd by remember(endDayOfWeek) { mutableStateOf(endDayOfWeek) }
    val startIndex = days.indexOf(selectedStart).coerceAtLeast(0)
    val endIndex = days.indexOf(selectedEnd).coerceAtLeast(0)
    val wheelHeight = WheelItemHeight * WheelVisibleItemCount.toFloat()

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
                text = stringResource(R.string.day_of_week_range_picker_label_start),
                modifier = Modifier.weight(1f),
            )
            WheelLabel(
                text = stringResource(R.string.day_of_week_range_picker_label_end),
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
                    items = days,
                    selectedIndex = startIndex,
                    onSelectedIndexChange = { index -> selectedStart = days[index] },
                    modifier = Modifier.weight(1f),
                ) { day, isSelected ->
                    WheelItem(
                        text = dayOfWeekText(day),
                        selected = isSelected,
                        selectedContentColor = colors.selectedContentColor,
                        contentColor = colors.contentColor,
                    )
                }
                PickerWheel(
                    items = days,
                    selectedIndex = endIndex,
                    onSelectedIndexChange = { index -> selectedEnd = days[index] },
                    modifier = Modifier.weight(1f),
                ) { day, isSelected ->
                    WheelItem(
                        text = dayOfWeekText(day),
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
                onClick = { onConfirm(selectedStart, selectedEnd) },
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacing4)),
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayOfWeekRangePickerModalBottomSheet(
    startDayOfWeek: DayOfWeek,
    endDayOfWeek: DayOfWeek,
    onHide: () -> Unit,
    onConfirm: (DayOfWeek, DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
        DayOfWeekRangePicker(
            startDayOfWeek = startDayOfWeek,
            endDayOfWeek = endDayOfWeek,
            onConfirm = { start, end ->
                onConfirm(start, end)
                hideSheetAnimated()
            },
            onDismiss = hideSheetAnimated,
            dismissButton = { TextButton(onClick = it) { Text(stringResource(R.string.common_action_cancel)) } },
            title = {
                Text(
                    text = stringResource(R.string.day_of_week_range_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            },
        )
    }
}
