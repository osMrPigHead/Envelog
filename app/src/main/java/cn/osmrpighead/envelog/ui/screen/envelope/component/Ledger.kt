package cn.osmrpighead.envelog.ui.screen.envelope.component

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
    onBudgetClick: (BudgetId, LocalDate?) -> Unit,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.spacing4)),
    ) {
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
                EnvelopeTab.Budget -> BudgetList(
                    budgets = ledgerState.budgets,
                    yearMonth = yearMonth,
                    onBudgetClick = { onBudgetClick(it, null) },
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.spacing4),
                        bottom = dimensionResource(R.dimen.spacing8),
                    ),
                )

                EnvelopeTab.Allocation -> AllocationList(
                    allocations = ledgerState.allocations,
                    modifier = Modifier.padding(
                        top = dimensionResource(R.dimen.spacing4),
                        bottom = dimensionResource(R.dimen.spacing8),
                    ),
                )

                EnvelopeTab.Calendar -> EnvelopeCalendar(
                    model = ledgerState.calendar,
                    onDateClick = onDateClick,
                    onBudgetClick = onBudgetClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing8)),
                )
            }
        }}
}
