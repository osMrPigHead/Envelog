package cn.osmrpighead.envelog.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.SubcomposeLayout

@Composable
fun MeasureInvisible(
    onMeasured: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val placeable = subcompose(Unit, content)
            .first()
            .measure(
                constraints.copy(
                    minHeight = 0,
                ),
            )
        onMeasured(placeable.height)
        layout(0, 0) {}
    }
}
