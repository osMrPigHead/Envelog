package cn.osmrpighead.envelog.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.TextAutoSizeDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.util.annotation.LangPreviews

@Immutable
data class StatItemStyle(
    val horizontalAlignment: Alignment.Horizontal,
    val titleStyle: TextStyle,
    val valueStyle: TextStyle,
    val valueAutoSize: TextAutoSize,
    val captionStyle: TextStyle,
) {
    enum class Size { Extreme, Large, Medium }

    companion object {
        @Composable
        fun of(size: Size, horizontalAlignment: Alignment.Horizontal) = when (size) {
            Size.Extreme -> extreme(horizontalAlignment)
            Size.Large -> large(horizontalAlignment)
            Size.Medium -> medium(horizontalAlignment)
        }

        @Composable
        private fun extreme(horizontalAlignment: Alignment.Horizontal) = StatItemStyle(
            horizontalAlignment = horizontalAlignment,
            titleStyle = MaterialTheme.typography.labelMedium.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.Both,
                    mode = LineHeightStyle.Mode.Tight,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            valueStyle = MaterialTheme.typography.headlineSmall.copy(
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Bottom,
                    trim = LineHeightStyle.Trim.Both,
                    mode = LineHeightStyle.Mode.Tight,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            ),
            valueAutoSize = TextAutoSize.StepBased(
                minFontSize = TextAutoSizeDefaults.MinFontSize,
                maxFontSize = MaterialTheme.typography.headlineSmall.fontSize,
            ),
            captionStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        @Composable
        private fun large(horizontalAlignment: Alignment.Horizontal) = StatItemStyle(
            horizontalAlignment = horizontalAlignment,
            titleStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            valueStyle = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            valueAutoSize = TextAutoSize.StepBased(
                minFontSize = TextAutoSizeDefaults.MinFontSize,
                maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
            ),
            captionStyle = MaterialTheme.typography.labelMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )

        @Composable
        private fun medium(horizontalAlignment: Alignment.Horizontal) = StatItemStyle(
            horizontalAlignment = horizontalAlignment,
            titleStyle = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            valueStyle = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            valueAutoSize = TextAutoSize.StepBased(
                minFontSize = TextAutoSizeDefaults.MinFontSize,
                maxFontSize = MaterialTheme.typography.titleLarge.fontSize,
            ),
            captionStyle = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }
}

@Composable
fun StatItemStyle.rememberValueHeight(sampleText: String = "0"): Dp {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    return remember(sampleText, density, valueStyle) {
        with(density) {
            textMeasurer.measure(
                text = sampleText,
                style = valueStyle,
                maxLines = 1,
                softWrap = false,
            ).size.height.toDp()
        }
    }
}

@Composable
fun StatItem(
    style: StatItemStyle,
    modifier: Modifier = Modifier,
    valueHeight: Dp? = null,
    title: String? = null,
    value: AnnotatedString? = null,
    caption: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = style.horizontalAlignment,
    ) {
        if (title != null) Text(
            text = title,
            style = style.titleStyle,
        )
        if (value != null) Box(
            modifier = Modifier
                .height(valueHeight ?: style.rememberValueHeight()),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = value,
                maxLines = 1,
                style = style.valueStyle,
                autoSize = style.valueAutoSize,
            )
        }
        if (caption != null) Text(
            text = caption,
            style = style.captionStyle,
        )
    }
}

@Composable
fun StatItem(
    style: StatItemStyle,
    modifier: Modifier = Modifier,
    valueHeight: Dp? = null,
    title: String? = null,
    value: String? = null,
    caption: String? = null,
) = StatItem(
    style,
    modifier,
    valueHeight,
    title,
    if (value != null) AnnotatedString(value) else null,
    caption,
)

@LangPreviews
@Composable
private fun WithHintPreview() {
    StatItem(
        style = StatItemStyle.of(StatItemStyle.Size.Large, Alignment.Start),
        title = stringResource(R.string.summary_remaining_title),
        value = "1145.14",
        caption = stringResource(R.string.summary_budgetRemaining_caption, 91),
    )
}

@LangPreviews
@Composable
private fun WithAdditionPreview() {
    StatItem(
        style = StatItemStyle.of(StatItemStyle.Size.Medium, Alignment.End),
        title = "今日可用",
        value = buildAnnotatedString {
            append("46\u200a")
            withStyle(MaterialTheme.typography.labelMedium.toSpanStyle()) { append("+16") }
        },
    )
}
