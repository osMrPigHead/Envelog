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
import cn.osmrpighead.envelog.ui.screen.envelope.envelopeEntry
import cn.osmrpighead.envelog.ui.screen.overview.overviewEntry
import cn.osmrpighead.envelog.ui.screen.record.recordEntry

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
                onNavigateToEnvelopeWithId = { key, id ->
                    backStack.navigate(key, AppRoute.Envelope(NavigationTarget.Existing(id)))
                },
                onNavigateToEnvelopeWithoutId = { backStack.navigate(it, AppRoute.Envelope(NavigationTarget.New())) },
                onNavigateToRecord = { key, id -> backStack.navigate(key, AppRoute.Record(id)) },
            )
            recordEntry(cancel = { backStack.pop(it) })
            envelopeEntry(cancel = { backStack.pop(it) })
        },
        transitionSpec = {
            slideInHorizontally(initialOffsetX = { it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                    slideOutHorizontally(targetOffsetX = { it })
        },
    )
}
