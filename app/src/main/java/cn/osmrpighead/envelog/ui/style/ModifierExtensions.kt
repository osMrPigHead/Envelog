package cn.osmrpighead.envelog.ui.style

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier

context(scope: RowScope)
fun Modifier.radialFilledWeight(weight: Float, fill: Boolean = true): Modifier = with(scope) {
    this@radialFilledWeight.weight(weight, fill).fillMaxHeight()
}

context(scope: ColumnScope)
fun Modifier.radialFilledWeight(weight: Float, fill: Boolean = true): Modifier = with(scope) {
    this@radialFilledWeight.weight(weight, fill).fillMaxWidth()
}
