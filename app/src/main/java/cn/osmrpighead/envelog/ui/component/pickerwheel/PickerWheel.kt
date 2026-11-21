package cn.osmrpighead.envelog.ui.component.pickerwheel

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import kotlinx.coroutines.flow.filter

internal val WheelItemHeight @Composable get() = dimensionResource(R.dimen.wheelItemHeight)
internal const val WheelVisibleItemCount = 5
private const val WheelMaxFlingItems = 30

@Composable
internal fun WheelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
internal fun WheelItem(
    text: String,
    selected: Boolean,
    selectedContentColor: Color,
    contentColor: Color,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = if (selected) selectedContentColor else contentColor,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

@Composable
internal fun <T> PickerWheel(
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
