package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute

fun EntryProviderScope<AppRoute>.envelopeEntry(
    cancel: (AppRoute.Envelope) -> Unit,
) {
    entry<AppRoute.Envelope> { key ->
        val viewModel: EnvelopeViewModel = hiltViewModel<EnvelopeViewModel, EnvelopeViewModel.Factory>(
            creationCallback = { it.create(key) },
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EnvelopeScreen(
            uiState = uiState,
            initializeForNew = viewModel::initializeForNew,
            updateEnvelope = viewModel::updateEnvelope,
            openActiveBudget = viewModel::openActiveBudget,
            newActiveBudget = viewModel::newActiveBudget,
            updateActiveBudget = viewModel::updateActiveBudget,
            closeActiveBudget = viewModel::closeActiveBudget,
            saveActiveBudget = viewModel::saveActiveBudget,
            cancel = { cancel(key) },
            save = viewModel::save,
        )
    }
}
