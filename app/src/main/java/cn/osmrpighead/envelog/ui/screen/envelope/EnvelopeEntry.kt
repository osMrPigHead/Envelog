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
            creationCallback = { it.create(key) { back(key) } },
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EnvelopeScreen(
            uiState = uiState,
            selectMonth = viewModel::selectMonth,
            initializeForNew = viewModel::initializeForNew,
            updateEnvelope = viewModel::updateEnvelope,
            openActiveBudget = viewModel::openActiveBudget,
            newActiveBudget = viewModel::newActiveBudget,
            updateActiveBudget = viewModel::updateActiveBudget,
            closeActiveBudget = viewModel::closeActiveBudget,
            saveActiveBudget = viewModel::saveActiveBudget,
            deleteActiveBudget = viewModel::deleteActiveBudget,
            clearActiveBudget = viewModel::clearActiveBudget,
            saveAndBack = viewModel::saveAndBack,
            archiveAndBack = viewModel::archiveAndBack,
            clearSubmissionError = viewModel::clearSubmissionError,
            openQuickSet = viewModel::openQuickSet,
            dismissQuickSet = viewModel::dismissQuickSet,
            applyQuickSet = viewModel::applyQuickSet,
            confirmQuickSetOverwrite = viewModel::confirmQuickSetOverwrite,
            cancelQuickSetOverwrite = viewModel::cancelQuickSetOverwrite,
            clearQuickSet = viewModel::clearQuickSet,
        )
    }
}
