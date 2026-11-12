package cn.osmrpighead.envelog.ui.screen.record

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute

fun EntryProviderScope<AppRoute>.recordEntry(
    back: (AppRoute.Record) -> Unit,
) {
    entry<AppRoute.Record> { key ->
        val viewModel: RecordViewModel = hiltViewModel<RecordViewModel, RecordViewModel.Factory>(
            creationCallback = { it.create(key) },
        )
        RecordScreen(
            uiState = viewModel.uiState,
            back = { back(key) },
            backEvent = viewModel.backEvent,
        )
    }
}
