package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.nextOrSame
import cn.osmrpighead.envelog.core.time.previousOrSame
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface BudgetDisplay {
    val length: Long

    sealed interface NoSpace : BudgetDisplay {
        val amount: Long
        val id: BudgetId
    }

    data class Internal(override val length: Long, override val amount: Long, override val id: BudgetId) : NoSpace
    data class External(override val length: Long, override val amount: Long, override val id: BudgetId) : NoSpace
    data class LeftBound(override val length: Long, override val amount: Long, override val id: BudgetId) : NoSpace
    data class RightBound(override val length: Long, override val amount: Long, override val id: BudgetId) : NoSpace
    data class Spacer(override val length: Long) : BudgetDisplay
}

data class BudgetCalendarWeekModel(
    val before: Long?,
    val after: Long?,
    val span: HalfOpenDateSpan,
    val budgets: List<BudgetDisplay>,
)

data class BudgetCalendarModel(
    val weeks: List<BudgetCalendarWeekModel>,
    val allocations: Map<LocalDate, DailyAllocationSummary>,
) {
    companion object {
        fun of(
            span: HalfOpenDateSpan,
            firstDayOfWeek: DayOfWeek,
            budgets: List<Budget>,
            allocations: Map<LocalDate, DailyAllocationSummary>,
        ): BudgetCalendarModel {
            return BudgetCalendarModel(
                span.splitIntoWeeks(firstDayOfWeek).map { week ->
                    val firstDayOfThisWeek = week.startDate.previousOrSame(firstDayOfWeek)
                    val firstDayOfNextWeek = week.endDate.nextOrSame(firstDayOfWeek)
                    BudgetCalendarWeekModel(
                        (firstDayOfThisWeek countDaysTo week.startDate).takeIf { it != 0L },
                        (week.endDate countDaysTo firstDayOfNextWeek).takeIf { it != 0L },
                        week,
                        buildList {
                            var date = firstDayOfThisWeek
                            budgets.filter { it.dateSpan overlaps week }.sortedBy { it.dateSpan.startDate }.forEach {
                                val intersect = it.dateSpan intersect week
                                if (date < intersect.startDate) add(BudgetDisplay.Spacer(date countDaysTo intersect.startDate))
                                when (Pair(it.dateSpan hasBefore intersect, it.dateSpan hasAfter intersect)) {
                                    true to true -> add(BudgetDisplay.External(intersect.length, it.amount, it.id))
                                    false to true -> add(BudgetDisplay.LeftBound(intersect.length, it.amount, it.id))
                                    true to false -> add(BudgetDisplay.RightBound(intersect.length, it.amount, it.id))
                                    else -> add(BudgetDisplay.Internal(intersect.length, it.amount, it.id))
                                }
                                date = intersect.endDate
                            }
                            if (date < firstDayOfNextWeek) add(BudgetDisplay.Spacer(date countDaysTo firstDayOfNextWeek))
                        },
                    )
                },
                allocations,
            )
        }
    }
}
