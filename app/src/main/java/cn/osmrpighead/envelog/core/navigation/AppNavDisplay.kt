package cn.osmrpighead.envelog.core.navigation

import androidx.compose.animation.core.tween
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
import cn.osmrpighead.envelog.ui.theme.MotionTokens

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
            )
            envelopeEntry(back = { backStack.pop(it) })
        },
        transitionSpec = {
            slideInHorizontally(
                animationSpec = tween(MotionTokens.DurationNavSlide),
                initialOffsetX = { it },
            ) togetherWith
                    slideOutHorizontally(animationSpec = tween(MotionTokens.DurationNavSlide), targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(MotionTokens.DurationNavSlide),
                initialOffsetX = { -it },
            ) togetherWith slideOutHorizontally(
                animationSpec = tween(MotionTokens.DurationNavSlide),
                targetOffsetX = { it },
            )
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(
                animationSpec = tween(MotionTokens.DurationNavSlide),
                initialOffsetX = { -it },
            ) togetherWith slideOutHorizontally(
                animationSpec = tween(MotionTokens.DurationNavSlide),
                targetOffsetX = { it },
            )
        },
    )
}
