package cn.osmrpighead.envelog.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImeInvisibleBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    BackHandler(
        enabled = enabled && !WindowInsets.isImeVisible,
        onBack = onBack,
    )
}
