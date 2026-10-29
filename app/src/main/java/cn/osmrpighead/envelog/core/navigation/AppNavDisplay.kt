package cn.osmrpighead.envelog.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import cn.osmrpighead.envelog.ui.screen.envelope.envelopeSheetEntry
import cn.osmrpighead.envelog.ui.screen.overview.overviewEntry

@Composable
fun AppNavDisplay(backStack: NavBackStack<AppRoute>) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            overviewEntry(
                onNavigateToEnvelopeWithId = { id -> backStack.add(AppRoute.Envelope(NavigationTarget.Existing(id))) },
                onNavigateToEnvelopeWithoutId = { backStack.add(AppRoute.Envelope(NavigationTarget.New())) },
            )
            envelopeSheetEntry()
        },
    )
}
