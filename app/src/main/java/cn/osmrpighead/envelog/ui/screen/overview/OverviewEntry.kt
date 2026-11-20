package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.data.model.EnvelopeId

fun EntryProviderScope<AppRoute>.overviewEntry(
    onNavigateToEnvelopeWithId: (AppRoute.Overview, EnvelopeId) -> Unit,
    onNavigateToEnvelopeWithoutId: (AppRoute.Overview) -> Unit,
) {
    entry<AppRoute.Overview> { key ->
        val viewModel: OverviewViewModel = hiltViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        OverviewScreen(
            uiState = uiState,
            selectMonth = viewModel::selectMonth,
            archiveEnvelope = viewModel::archiveEnvelope,
            deleteEnvelope = viewModel::deleteEnvelope,
            requireExpenseFormState = viewModel::requireExpenseFormState,
            submitExpense = viewModel::submitExpense,
            editEnvelope = { onNavigateToEnvelopeWithId(key, it) },
            newEnvelope = { onNavigateToEnvelopeWithoutId(key) },
        )
    }
}
