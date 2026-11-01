package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.data.model.EnvelopeId

fun EntryProviderScope<AppRoute>.overviewEntry(
    onNavigateToEnvelopeWithId: (EnvelopeId) -> Unit,
    onNavigateToEnvelopeWithoutId: () -> Unit,
    onNavigateToRecord: (EnvelopeId) -> Unit,
) {
    entry<AppRoute.Overview> {
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        OverviewScreen(
            uiState = uiState,
            onArchiveClick = viewModel::archiveEnvelope,
            onEditClick = onNavigateToEnvelopeWithId,
            onNewClick = onNavigateToEnvelopeWithoutId,
            onEnvelopeClick = onNavigateToRecord,
        )
    }
}
