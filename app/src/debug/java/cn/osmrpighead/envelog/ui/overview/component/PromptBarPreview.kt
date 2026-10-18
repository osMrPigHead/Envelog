package cn.osmrpighead.envelog.ui.overview.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@LangPreview
@Composable
private fun PromptBarPreview() {
    EnvelogTheme {
        var managing by remember { mutableStateOf(false) }
        PromptBar(
            managing = managing,
            onManageButtonClick = { managing = !managing },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
