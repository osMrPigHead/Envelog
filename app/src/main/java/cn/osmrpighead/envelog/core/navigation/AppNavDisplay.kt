package cn.osmrpighead.envelog.core.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import cn.osmrpighead.envelog.ui.screen.overview.overviewEntry
import cn.osmrpighead.envelog.ui.screen.record.recordEntry
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun AppNavDisplay(backStack: NavBackStack<AppRoute>) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            overviewEntry(
                onNavigateToEnvelopeWithId = {},
                onNavigateToEnvelopeWithoutId = {},
                onNavigateToRecord = { backStack.add(AppRoute.Record(it)) },
            )
            recordEntry(cancel = { backStack.removeLastOrNull() })
        },
    )
}
