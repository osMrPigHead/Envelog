package cn.osmrpighead.envelog.ui.screen.envelope.component.ledger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.screen.envelope.LedgerState
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.time.LocalDate
import java.time.YearMonth

enum class EnvelopeTab { Calendar, Budget, Allocation }

@Composable
fun Ledger(
    yearMonth: YearMonth,
    ledgerState: LedgerState,
    activeTab: EnvelopeTab,
    onBudgetClick: (BudgetId) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AnimatedContent(
            targetState = activeTab,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                val enter = slideInHorizontally(
                    animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                    initialOffsetX = { width -> direction * width / 4 },
                ) + fadeIn(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                val exit = slideOutHorizontally(
                    animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                    targetOffsetX = { width -> -direction * width / 4 },
                ) + fadeOut(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                (enter togetherWith exit).using(null)
            },
        ) { tab ->
            when (tab) {
                EnvelopeTab.Calendar -> EnvelopeCalendar(
                    model = ledgerState.calendar,
                    onClick = onDateClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.space12))
                        .verticalScroll(rememberScrollState()),
                )

                EnvelopeTab.Budget -> BudgetList(
                    budgets = ledgerState.budgets,
                    yearMonth = yearMonth,
                    onBudgetClick = onBudgetClick,
                )

                EnvelopeTab.Allocation -> AllocationList(
                    allocations = ledgerState.allocations,
                )
            }
        }
    }
}
