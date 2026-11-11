package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.LocalDate
import java.time.YearMonth

enum class QuickSetMode {
    Daily,
    MonthlyTotal,
}

data class QuickSetBudgetPlan(
    val targetSpan: HalfOpenDateSpan,
    val totalAmount: Long,
    val budgets: List<Budget>,
    val conflicts: List<Budget>,
)

object QuickSetBudgetPlanner {
    fun targetSpan(
        mode: QuickSetMode,
        yearMonth: YearMonth,
        today: LocalDate,
    ): HalfOpenDateSpan? {
        val todayMonth = YearMonth.from(today)
        if (yearMonth < todayMonth) return null

        val monthSpan = yearMonth.toHalfOpenDateSpan()
        return when (mode) {
            QuickSetMode.Daily -> {
                val start = if (yearMonth == todayMonth) maxOf(today, monthSpan.startDate) else monthSpan.startDate
                start untilExclusive monthSpan.endDate
            }

            QuickSetMode.MonthlyTotal -> monthSpan
        }
    }

    fun totalAmount(
        mode: QuickSetMode,
        amount: Long,
        span: HalfOpenDateSpan,
    ): Long? {
        if (amount <= 0) return null
        val days = span.length
        if (days <= 0) return null
        return when (mode) {
            QuickSetMode.Daily -> if (amount > Long.MAX_VALUE / days) null else amount * days
            QuickSetMode.MonthlyTotal -> amount
        }
    }

    fun plan(
        mode: QuickSetMode,
        amount: Long,
        yearMonth: YearMonth,
        today: LocalDate,
        envelopeId: EnvelopeId,
        existing: Collection<Budget>,
        newId: () -> BudgetId = { BudgetId.generate() },
    ): QuickSetBudgetPlan? {
        val span = targetSpan(mode, yearMonth, today) ?: return null
        val total = totalAmount(mode, amount, span) ?: return null

        val budgets = when (mode) {
            QuickSetMode.Daily -> buildList {
                for (date in span) {
                    add(
                        Budget(
                            id = newId(),
                            envelopeId = envelopeId,
                            dateSpan = date untilExclusive date.plusDays(1),
                            attributedMonth = yearMonth,
                            amount = amount,
                        ),
                    )
                }
            }

            QuickSetMode.MonthlyTotal -> listOf(
                Budget(
                    id = newId(),
                    envelopeId = envelopeId,
                    dateSpan = span,
                    attributedMonth = yearMonth,
                    amount = amount,
                ),
            )
        }

        val conflicts = existing.filter { it.envelopeId == envelopeId && it.dateSpan overlaps span }
        return QuickSetBudgetPlan(
            targetSpan = span,
            totalAmount = total,
            budgets = budgets,
            conflicts = conflicts,
        )
    }
}
