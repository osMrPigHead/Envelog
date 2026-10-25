package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.navigation.AppRoute
import java.util.UUID

fun EntryProviderScope<AppRoute>.overviewEntry(
    onNavigateToEnvelope: (UUID) -> Unit,
) {
    entry<AppRoute.Overview> {
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        OverviewScreen(
            uiState = uiState,
            onArchiveClick = viewModel::archiveEnvelope,
            onEditClick = onNavigateToEnvelope,
        )
    }
}
