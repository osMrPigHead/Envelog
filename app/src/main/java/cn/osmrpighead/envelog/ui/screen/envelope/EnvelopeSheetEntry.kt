package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.navigation.AppRoute


fun EntryProviderScope<AppRoute>.envelopeSheetEntry() {
    entry<AppRoute.Envelope> { key ->
        val viewModel: EnvelopeSheetViewModel = hiltViewModel<EnvelopeSheetViewModel, EnvelopeSheetViewModel.Factory>(
            creationCallback = { it.create(key) }
        )
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        EnvelopeSheetScreen(
            uiState = uiState,
            onNameChange = {},
            onBudgetedChange = {},
            onDateClicked = {},
            onBudgetItemSheetDismissed = {},
            onPeriodChange = {},
            onAmountChange = {},
        )
    }
}
