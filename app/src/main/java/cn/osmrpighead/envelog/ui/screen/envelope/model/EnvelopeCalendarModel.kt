package cn.osmrpighead.envelog.ui.screen.envelope.model

import android.util.Log
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.nextOrSame
import cn.osmrpighead.envelog.core.time.previousOrSame
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import java.time.DayOfWeek
import java.time.LocalDate

enum class BudgetDisplayType {
    Internal, External, LeftBound, RightBound
}

data class BudgetDisplay(
    val offset: Float,
    val length: Float,
    val type: BudgetDisplayType,
)

data class EnvelopeCalendarWeekModel(
    val before: Float,
    val after: Float,
    val span: HalfOpenDateSpan,
    val budgets: List<BudgetDisplay>,
)

data class EnvelopeCalendarModel(
    val weeks: List<EnvelopeCalendarWeekModel>,
    val isBudgeted: Boolean,
    val allocations: Map<LocalDate, DailyAllocationSummary>,
) {
    companion object {
        fun of(
            span: HalfOpenDateSpan,
            firstDayOfWeek: DayOfWeek,
            isBudgeted: Boolean,
            budgets: Collection<Budget>,
            allocations: Map<LocalDate, DailyAllocationSummary>,
        ): EnvelopeCalendarModel {
            Log.d("Xterfusion", "RECALCULATED")
            val weeks = span.splitIntoWeeks(firstDayOfWeek).map { weekSpan ->
                val weekStart = weekSpan.startDate.previousOrSame(firstDayOfWeek)
                val weekEnd = weekSpan.endDate.nextOrSame(firstDayOfWeek)
                EnvelopeCalendarWeekModel(
                    before = (weekStart countDaysTo weekSpan.startDate).toFloat(),
                    after = (weekSpan.endDate countDaysTo weekEnd).toFloat(),
                    span = weekSpan,
                    budgets = budgets
                        .map { Pair(weekSpan intersect it.dateSpan, it.dateSpan) }
                        .filter { it.first.length > 0 }
                        .map { (intersect, full) ->
                            BudgetDisplay(
                                offset = (weekStart countDaysTo intersect.startDate).toFloat(),
                                length = intersect.length.toFloat(),
                                type = when (Pair(full hasBefore intersect, full hasAfter intersect)) {
                                    Pair(true, true) -> BudgetDisplayType.External
                                    Pair(true, false) -> BudgetDisplayType.RightBound
                                    Pair(false, true) -> BudgetDisplayType.LeftBound
                                    else -> BudgetDisplayType.Internal
                                },
                            )
                        },
                )
            }

            return EnvelopeCalendarModel(weeks, isBudgeted, allocations)
        }
    }
}
