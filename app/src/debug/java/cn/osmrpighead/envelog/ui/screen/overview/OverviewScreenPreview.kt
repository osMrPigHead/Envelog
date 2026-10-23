package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme

@Preview
@Composable
private fun OverviewScreenPreview() {
    EnvelogTheme(dynamicColor = false) {
        OverviewScreen(
            uiState = OverviewPreviewData.uiState,
            onArchiveClick = {},
            onEditClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Preview(name = "Overview (empty)", showBackground = true)
@Composable
private fun OverviewScreenEmptyPreview() {
    EnvelogTheme(dynamicColor = false) {
        OverviewScreen(
            uiState = OverviewPreviewData.emptyUiState,
            onArchiveClick = {},
            onEditClick = {},
            modifier = Modifier.fillMaxSize(),
        )
    }
}
