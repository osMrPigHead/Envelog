package cn.osmrpighead.envelog.ui.style

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

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
