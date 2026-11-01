package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import cn.osmrpighead.envelog.R

@Composable
fun LabeledBox(
    label: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = dimensionResource(R.dimen.cornerRadiusLarge),
    borderWidth: Dp = dimensionResource(R.dimen.borderWidth),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    labelColor: Color = MaterialTheme.colorScheme.primary,
    labelBackground: Color = MaterialTheme.colorScheme.surface,
    contentPadding: PaddingValues = PaddingValues(dimensionResource(R.dimen.spacingBold)),
    content: @Composable ColumnScope.() -> Unit
) {
    val labelSize = remember { intArrayOf(0, 0) }
    val startGap = dimensionResource(R.dimen.spacingBold)

    Layout(
        modifier = modifier.drawBehind {
            val h = labelSize[1]
            if (h == 0) return@drawBehind
            val stroke = borderWidth.toPx()
            val top = h / 2f
            val inset = stroke / 2f
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(inset, top),
                size = Size(size.width - stroke, size.height - top - inset),
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Stroke(width = stroke)
            )
        },
        content = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier
                    .background(labelBackground)
                    .padding(horizontal = dimensionResource(R.dimen.spacingMedium))
            )
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content
            )
        }
    ) { measurables, constraints ->
        val labelPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val contentPlaceable = measurables[1].measure(constraints)

        labelSize[0] = labelPlaceable.width
        labelSize[1] = labelPlaceable.height

        val half = labelPlaceable.height / 2
        val startGap = startGap.roundToPx()
        val width = maxOf(contentPlaceable.width, labelPlaceable.width + startGap * 2)
        val height = half + contentPlaceable.height

        layout(width, height) {
            labelPlaceable.place(startGap, 0)
            contentPlaceable.place(0, half)
        }
    }
}
