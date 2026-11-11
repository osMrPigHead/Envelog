package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.text.dayOfWeekText
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

sealed interface DayOfWeekRangePickerSheetBuilder {
    @Composable
    fun Composable(
        startDayOfWeek: DayOfWeek,
        endDayOfWeek: DayOfWeek,
        hideSheet: () -> Unit,
        modifier: Modifier = Modifier,
    )

    class Custom(
        private val composable: @Composable (
            startDayOfWeek: DayOfWeek,
            endDayOfWeek: DayOfWeek,
            hideSheet: () -> Unit,
            modifier: Modifier,
        ) -> Unit,
    ) : DayOfWeekRangePickerSheetBuilder {
        @Composable
        override fun Composable(
            startDayOfWeek: DayOfWeek,
            endDayOfWeek: DayOfWeek,
            hideSheet: () -> Unit,
            modifier: Modifier,
        ) = composable(startDayOfWeek, endDayOfWeek, hideSheet, modifier)
    }

    class Default(
        private val confirmEventHandler: DayOfWeekRangePickerConfirmEventHandler,
        private val onDismiss: (hideSheet: () -> Unit) -> Unit = { it() },
        private val dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)? = {
            TextButton(onClick = it) { Text(stringResource(R.string.common_action_cancel)) }
        },
        private val title: (@Composable () -> Unit)? = {
            Text(
                text = stringResource(R.string.day_of_week_range_picker_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        private val colors: DayOfWeekRangePickerColors? = null,
    ) : DayOfWeekRangePickerSheetBuilder {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Composable(
            startDayOfWeek: DayOfWeek,
            endDayOfWeek: DayOfWeek,
            hideSheet: () -> Unit,
            modifier: Modifier,
        ) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()
            val hideSheetAnimated: () -> Unit = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { hideSheet() }
            }

            LaunchedEffect(sheetState) { sheetState.show() }

            ModalBottomSheet(
                onDismissRequest = { onDismiss(hideSheetAnimated) },
                sheetState = sheetState,
                modifier = modifier,
            ) {
                DayOfWeekRangePickerSheetContent(
                    startDayOfWeek = startDayOfWeek,
                    endDayOfWeek = endDayOfWeek,
                    onConfirm = { start, end -> confirmEventHandler.onConfirm(start, end, hideSheetAnimated) },
                    onDismiss = hideSheetAnimated,
                    dismissButton = dismissButton,
                    title = title,
                    colors = colors ?: DayOfWeekRangePickerDefaults.colors(),
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekRangePickerSheetContent(
    startDayOfWeek: DayOfWeek,
    endDayOfWeek: DayOfWeek,
    onConfirm: (DayOfWeek, DayOfWeek) -> Unit,
    onDismiss: () -> Unit,
    dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)?,
    title: (@Composable () -> Unit)?,
    colors: DayOfWeekRangePickerColors,
    modifier: Modifier = Modifier,
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
            .padding(horizontal = dimensionResource(R.dimen.spacingBold))
            .padding(bottom = dimensionResource(R.dimen.spacingBold)),
    ) {
        title?.invoke()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingSmall)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
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
                .padding(top = dimensionResource(R.dimen.spacingMini))
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
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
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
                .padding(top = dimensionResource(R.dimen.spacingBold)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismissButton?.invoke(onDismiss)
            Button(
                onClick = { onConfirm(selectedStart, selectedEnd) },
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}
