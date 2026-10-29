package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.navigation.AppRoute


@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<AppRoute>.envelopeSheetEntry() {
    entry<AppRoute.Envelope> { key ->
        val viewModel: EnvelopeSheetViewModel = hiltViewModel<EnvelopeSheetViewModel, EnvelopeSheetViewModel.Factory>(
            creationCallback = { it.create(key) }
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EnvelopeSheetScreen(
            uiState = uiState,
            onNameChange = viewModel::onNameChange,
            onBudgetedChange = viewModel::onBudgetedChange,
            onDateClicked = viewModel::onDateClicked,
            confirmBudgetItemSheetChange = viewModel::confirmBudgetItemSheetChange,
            onPeriodChange = viewModel::onPeriodChange,
            onAmountChange = viewModel::onAmountChange,
            onSave = viewModel::onSaveBudgetItem,
        )
    }
}
