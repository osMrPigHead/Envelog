package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.osmrpighead.envelog.R
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.YearMonth

private val WheelItemHeight = 48.dp
private const val WheelVisibleItemCount = 5
private const val WheelMaxFlingItems = 30

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

sealed interface MonthPickerSheetBuilder {
    @Composable
    fun Composable(
        yearMonth: YearMonth,
        minYearMonth: YearMonth?,
        maxYearMonth: YearMonth?,
        hideSheet: () -> Unit,
        modifier: Modifier = Modifier,
    )

    class Custom(
        private val composable: @Composable (
            yearMonth: YearMonth,
            minYearMonth: YearMonth?,
            maxYearMonth: YearMonth?,
            hideSheet: () -> Unit,
            modifier: Modifier
        ) -> Unit,
    ) : MonthPickerSheetBuilder {
        @Composable
        override fun Composable(
            yearMonth: YearMonth,
            minYearMonth: YearMonth?,
            maxYearMonth: YearMonth?,
            hideSheet: () -> Unit,
            modifier: Modifier,
        ) = composable(yearMonth, minYearMonth, maxYearMonth, hideSheet, modifier)
    }

    class Default(
        private val confirmEventHandler: MonthPickerConfirmEventHandler,
        private val onDismiss: (hideSheet: () -> Unit) -> Unit = { it() },
        private val dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)? = {
            TextButton(onClick = it) { Text(stringResource(R.string.common_action_cancel)) }
        },
        private val title: (@Composable () -> Unit)? = {
            Text(
                text = stringResource(R.string.month_picker_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        private val colors: MonthPickerColors? = null,
    ) : MonthPickerSheetBuilder {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        override fun Composable(
            yearMonth: YearMonth,
            minYearMonth: YearMonth?,
            maxYearMonth: YearMonth?,
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
                MonthPickerSheetContent(
                    yearMonth = yearMonth,
                    minYearMonth = minYearMonth,
                    maxYearMonth = maxYearMonth,
                    onConfirm = { confirmEventHandler.onConfirm(it, hideSheetAnimated) },
                    onDismiss = hideSheetAnimated,
                    dismissButton = dismissButton,
                    title = title,
                    colors = colors ?: MonthPickerDefaults.colors(),
                )
            }
        }
    }
}

@Composable
private fun MonthPickerSheetContent(
    yearMonth: YearMonth,
    minYearMonth: YearMonth?,
    maxYearMonth: YearMonth?,
    onConfirm: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
    dismissButton: (@Composable (hideSheet: () -> Unit) -> Unit)?,
    title: (@Composable () -> Unit)?,
    colors: MonthPickerColors,
    modifier: Modifier = Modifier,
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
                Wheel(
                    items = years,
                    selectedIndex = yearIndex,
                    onSelectedIndexChange = { index -> selectYear(years[index]) },
                    modifier = Modifier.weight(1f),
                ) { year, isSelected ->
                    WheelItem(
                        text = year.toString(),
                        selected = isSelected,
                        colors = colors,
                    )
                }
                Wheel(
                    items = months,
                    selectedIndex = monthIndex,
                    onSelectedIndexChange = { index -> selectMonth(months[index]) },
                    modifier = Modifier.weight(1f),
                ) { month, isSelected ->
                    WheelItem(
                        text = month.toString().padStart(2, '0'),
                        selected = isSelected,
                        colors = colors,
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
                onClick = { onConfirm(selected) },
                enabled = range.contains(selected),
                modifier = Modifier.padding(start = dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(text = stringResource(R.string.common_action_ok))
            }
        }
    }
}

@Composable
private fun WheelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WheelItem(text: String, selected: Boolean, colors: MonthPickerColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) colors.selectedContentColor else colors.contentColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
private fun <T> Wheel(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T, Boolean) -> Unit,
) {
    if (items.isEmpty()) return

    val safeSelectedIndex = selectedIndex.coerceIn(items.indices)
    val pagerState = rememberPagerState(
        initialPage = safeSelectedIndex,
        pageCount = { items.size },
    )
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        pagerSnapDistance = PagerSnapDistance.atMost(WheelMaxFlingItems),
    )
    val latestSelectedIndex by rememberUpdatedState(safeSelectedIndex)
    val latestOnSelectedIndexChange by rememberUpdatedState(onSelectedIndexChange)

    LaunchedEffect(safeSelectedIndex, items.size) {
        if (pagerState.currentPage != safeSelectedIndex) {
            pagerState.animateScrollToPage(safeSelectedIndex)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.isScrollInProgress }
            .filter { scrolling -> !scrolling }
            .collect {
                val current = pagerState.currentPage
                if (current != latestSelectedIndex) latestOnSelectedIndexChange(current)
            }
    }

    val wheelHeight = WheelItemHeight * WheelVisibleItemCount.toFloat()
    VerticalPager(
        state = pagerState,
        modifier = modifier.height(wheelHeight),
        contentPadding = PaddingValues(vertical = (wheelHeight - WheelItemHeight) / 2f),
        pageSize = PageSize.Fixed(WheelItemHeight),
        beyondViewportPageCount = WheelVisibleItemCount / 2,
        flingBehavior = flingBehavior,
        snapPosition = SnapPosition.Center,
        overscrollEffect = null,
    ) { index ->
        val selected = index == pagerState.currentPage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = { onSelectedIndexChange(index) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            itemContent(items[index], selected)
        }
    }
}
