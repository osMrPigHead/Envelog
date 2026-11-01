package cn.osmrpighead.envelog.ui.screen.record

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import java.util.concurrent.atomic.AtomicBoolean

fun EntryProviderScope<AppRoute>.recordEntry(
    cancel: () -> Unit,
) {
    entry<AppRoute.Record> { key ->
        val navigated = remember { AtomicBoolean(false) }
        val viewModel: RecordViewModel = hiltViewModel<RecordViewModel, RecordViewModel.Factory>(
            creationCallback = { it.create(key, { if (navigated.compareAndSet(false, true)) cancel() }) },
        )
        RecordScreen(
            uiState = viewModel.uiState,
        )
    }
}
