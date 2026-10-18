package cn.osmrpighead.envelog.ui.component.statitem

import androidx.compose.ui.text.AnnotatedString

sealed interface StatItemSlot {
    data class Text(val text: AnnotatedString) : StatItemSlot {
        constructor(text: String) : this(AnnotatedString(text))
    }

    data object Space : StatItemSlot
    data object Empty : StatItemSlot
}

data class StatItemContent(
    val title: StatItemSlot,
    val value: StatItemSlot,
    val caption: StatItemSlot,
)
