package cn.osmrpighead.envelog.ui.style

import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import cn.osmrpighead.envelog.ui.theme.MotionTokens

fun Modifier.disableVerticalDrag(): Modifier = pointerInput(Unit) {
    detectVerticalDragGestures { change, _ ->
        change.consume()
    }
}

context(scope: RowScope)
fun Modifier.radialFilledWeight(weight: Float, fill: Boolean = true): Modifier = with(scope) {
    this@radialFilledWeight.weight(weight, fill).fillMaxHeight()
}

context(scope: ColumnScope)
fun Modifier.radialFilledWeight(weight: Float, fill: Boolean = true): Modifier = with(scope) {
    this@radialFilledWeight.weight(weight, fill).fillMaxWidth()
}

context(scope: LazyItemScope)
fun Modifier.animateItemWithToken(): Modifier = with(scope) {
    this@animateItemWithToken.animateItem(
        fadeInSpec = tween(MotionTokens.DurationSwitchFade),
        placementSpec = tween(MotionTokens.DurationSwitchSlide),
        fadeOutSpec = tween(MotionTokens.DurationSwitchFade),
    )
}
