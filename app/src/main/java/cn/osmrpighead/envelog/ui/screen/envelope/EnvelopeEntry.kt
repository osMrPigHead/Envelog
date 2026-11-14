package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute

fun EntryProviderScope<AppRoute>.envelopeEntry(
    back: (AppRoute.Envelope) -> Unit,
) {
    entry<AppRoute.Envelope> { key ->
        val viewModel: EnvelopeViewModel = hiltViewModel<EnvelopeViewModel, EnvelopeViewModel.Factory>(
            creationCallback = { it.create(key) },
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EnvelopeScreen(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            back = { back(key) },
        )
    }
}
