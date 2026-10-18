package cn.osmrpighead.envelog.ui.component.statitem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import cn.osmrpighead.envelog.ui.text.AutoSizedTextStyle

@Immutable
data class StatItemStyle(
    val containerAlignment: Alignment.Horizontal,
    val contentAlignment: Alignment,
    val title: AutoSizedTextStyle,
    val value: AutoSizedTextStyle,
    val caption: AutoSizedTextStyle,
) {
    enum class Size { Extreme, Large, Medium }

    companion object {
        @Composable
        fun of(size: Size, containerAlignment: Alignment.Horizontal, contentAlignment: Alignment) = when (size) {
            Size.Extreme -> extreme(containerAlignment, contentAlignment)
            Size.Large -> large(containerAlignment, contentAlignment)
            Size.Medium -> medium(containerAlignment, contentAlignment)
        }

        @Composable
        private fun extreme(containerAlignment: Alignment.Horizontal, contentAlignment: Alignment) = StatItemStyle(
            containerAlignment = containerAlignment,
            contentAlignment = contentAlignment,
            title = AutoSizedTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Bottom,
                        trim = LineHeightStyle.Trim.Both,
                        mode = LineHeightStyle.Mode.Tight,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
            value = AutoSizedTextStyle(
                MaterialTheme.typography.headlineSmall.copy(
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Bottom,
                        trim = LineHeightStyle.Trim.Both,
                        mode = LineHeightStyle.Mode.Tight,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ),
            caption = AutoSizedTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
        )

        @Composable
        private fun large(containerAlignment: Alignment.Horizontal, contentAlignment: Alignment) = StatItemStyle(
            containerAlignment = containerAlignment,
            contentAlignment = contentAlignment,
            title = AutoSizedTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
            value = AutoSizedTextStyle(
                MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ),
            caption = AutoSizedTextStyle(
                MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
        )

        @Composable
        private fun medium(containerAlignment: Alignment.Horizontal, contentAlignment: Alignment) = StatItemStyle(
            containerAlignment = containerAlignment,
            contentAlignment = contentAlignment,
            title = AutoSizedTextStyle(
                MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
            value = AutoSizedTextStyle(
                MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            ),
            caption = AutoSizedTextStyle(
                MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ),
        )

        val ExtremeCenterStart: StatItemStyle
            @Composable get() = of(Size.Extreme, Alignment.Start, Alignment.CenterStart)
        val ExtremeCenterEnd: StatItemStyle
            @Composable get() = of(Size.Extreme, Alignment.End, Alignment.CenterEnd)
        val LargeCenterStart: StatItemStyle
            @Composable get() = of(Size.Large, Alignment.Start, Alignment.CenterStart)
        val LargeCenterEnd: StatItemStyle
            @Composable get() = of(Size.Large, Alignment.End, Alignment.CenterEnd)
        val MediumCenterStart: StatItemStyle
            @Composable get() = of(Size.Medium, Alignment.Start, Alignment.CenterStart)
        val MediumCenterEnd: StatItemStyle
            @Composable get() = of(Size.Medium, Alignment.End, Alignment.CenterEnd)
    }
}

fun StatItemStyle.update(
    containerAlignment: Alignment.Horizontal? = null,
    contentAlignment: Alignment? = null,
    title: (AutoSizedTextStyle.() -> AutoSizedTextStyle)? = null,
    value: (AutoSizedTextStyle.() -> AutoSizedTextStyle)? = null,
    caption: (AutoSizedTextStyle.() -> AutoSizedTextStyle)? = null,
) = copy(
    containerAlignment = containerAlignment ?: this.containerAlignment,
    contentAlignment = contentAlignment ?: this.contentAlignment,
    title = if (title != null) this.title.run(title) else this.title,
    value = if (value != null) this.value.run(value) else this.value,
    caption = if (caption != null) this.caption.run(caption) else this.caption,
)
