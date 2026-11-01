package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.util.concurrent.atomic.AtomicBoolean

fun EntryProviderScope<AppRoute>.overviewEntry(
    onNavigateToEnvelopeWithId: (EnvelopeId) -> Unit,
    onNavigateToEnvelopeWithoutId: () -> Unit,
    onNavigateToRecord: (EnvelopeId) -> Unit,
) {
    entry<AppRoute.Overview> {
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val navigated = remember { AtomicBoolean(false) }
        OverviewScreen(
            uiState = uiState,
            onArchiveClick = viewModel::archiveEnvelope,
            onEditClick = { if (navigated.compareAndSet(false, true)) onNavigateToEnvelopeWithId(it) },
            onNewClick = { if (navigated.compareAndSet(false, true)) onNavigateToEnvelopeWithoutId() },
            onEnvelopeClick = { if (navigated.compareAndSet(false, true)) onNavigateToRecord(it) },
        )
    }
}
