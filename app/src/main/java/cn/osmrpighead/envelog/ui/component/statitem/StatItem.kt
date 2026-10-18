package cn.osmrpighead.envelog.ui.component.statitem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import cn.osmrpighead.envelog.ui.text.AutoSizedTextStyle
import cn.osmrpighead.envelog.ui.text.lineHeightAsDp

@Composable
private fun Slot(
    alignment: Alignment,
    style: AutoSizedTextStyle,
    slot: StatItemSlot,
    modifier: Modifier = Modifier,
) {
    when (slot) {
        is StatItemSlot.Text -> Box(
            modifier = modifier.height(style.lineHeightAsDp),
            contentAlignment = alignment,
        ) { Text(text = slot.text, maxLines = 1, style = style.textStyle, autoSize = style.autoSize) }

        is StatItemSlot.Space -> Spacer(modifier.height(style.lineHeightAsDp))
        is StatItemSlot.Empty -> {}
    }
}

@Composable
fun StatItem(
    style: StatItemStyle,
    content: StatItemContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = style.containerAlignment,
    ) {
        Slot(style.contentAlignment, style.title, content.title)
        Slot(style.contentAlignment, style.value, content.value)
        Slot(style.contentAlignment, style.caption, content.caption)
    }
}
