package cn.osmrpighead.envelog.ui.screen.record

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute

fun EntryProviderScope<AppRoute>.recordEntry(
    cancel: (AppRoute.Record) -> Unit,
) {
    entry<AppRoute.Record> { key ->
        val viewModel: RecordViewModel = hiltViewModel<RecordViewModel, RecordViewModel.Factory>(
            creationCallback = { it.create(key) { cancel(key) } },
        )
        RecordScreen(
            uiState = viewModel.uiState,
        )
    }
}
