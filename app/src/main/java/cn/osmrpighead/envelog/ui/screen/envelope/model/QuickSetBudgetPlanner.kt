package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.countDaysTo
import cn.osmrpighead.envelog.core.time.nextOrSame
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.DayOfWeek
import java.time.YearMonth

enum class QuickSetMode {
    Daily,
    Weekly,
    MonthlyTotal,
}

sealed interface QuickSetConfig {
    val amount: Long

    data class Daily(
        val intervalDays: Int,
        override val amount: Long,
    ) : QuickSetConfig

    data class Weekly(
        val startDayOfWeek: DayOfWeek,
        val endDayOfWeek: DayOfWeek,
        override val amount: Long,
    ) : QuickSetConfig

    data class MonthlyTotal(
        override val amount: Long,
    ) : QuickSetConfig
}

data class QuickSetSegment(
    val span: HalfOpenDateSpan,
    val amount: Long,
)

data class QuickSetSchedule(
    val targetSpan: HalfOpenDateSpan,
    val segments: List<QuickSetSegment>,
    val totalAmount: Long,
)

data class QuickSetBudgetPlan(
    val targetSpan: HalfOpenDateSpan,
    val totalAmount: Long,
    val budgets: List<Budget>,
    val conflicts: List<Budget>,
)

object QuickSetBudgetPlanner {
    fun schedule(
        config: QuickSetConfig,
        selection: ClosedDateSpan,
        yearMonth: YearMonth,
    ): QuickSetSchedule? {
        if (config.amount <= 0) return null

        val monthSpan = yearMonth.toHalfOpenDateSpan()
        val targetSpan = when (config) {
            is QuickSetConfig.MonthlyTotal -> monthSpan
            is QuickSetConfig.Daily, is QuickSetConfig.Weekly -> selection.toHalfOpen() intersect monthSpan
        }
        if (targetSpan.length <= 0) return null

        val segments = when (config) {
            is QuickSetConfig.Daily -> dailySegments(config, targetSpan)
            is QuickSetConfig.Weekly -> weeklySegments(config, targetSpan)
            is QuickSetConfig.MonthlyTotal -> listOf(QuickSetSegment(targetSpan, config.amount))
        }
        if (segments.isEmpty() || segments.any { it.amount <= 0 }) return null

        return QuickSetSchedule(
            targetSpan = targetSpan,
            segments = segments,
            totalAmount = totalAmountOrNull(segments) ?: return null,
        )
    }

    fun plan(
        config: QuickSetConfig,
        selection: ClosedDateSpan,
        yearMonth: YearMonth,
        envelopeId: EnvelopeId,
        existing: Collection<Budget>,
        newId: () -> BudgetId = { BudgetId.generate() },
    ): QuickSetBudgetPlan? {
        val schedule = schedule(config, selection, yearMonth) ?: return null

        val budgets = schedule.segments.map { segment ->
            Budget(
                id = newId(),
                envelopeId = envelopeId,
                dateSpan = segment.span,
                attributedMonth = yearMonth,
                amount = segment.amount,
            )
        }
        val conflicts = existing.filter { budget ->
            budget.envelopeId == envelopeId &&
                schedule.segments.any { segment -> budget.dateSpan overlaps segment.span }
        }.distinct()

        return QuickSetBudgetPlan(
            targetSpan = schedule.targetSpan,
            totalAmount = schedule.totalAmount,
            budgets = budgets,
            conflicts = conflicts,
        )
    }

    private fun dailySegments(
        config: QuickSetConfig.Daily,
        span: HalfOpenDateSpan,
    ): List<QuickSetSegment> {
        if (config.intervalDays <= 0) return emptyList()
        val periodDays = config.intervalDays.toLong()

        return buildList {
            var cursor = span.startDate
            var current = cursor untilExclusive cursor.plusDays(periodDays)
            while ((current intersect span).length >= periodDays / 2) {
                add(QuickSetSegment(current, config.amount))
                cursor = current.endDate
                current = cursor untilExclusive cursor.plusDays(periodDays)
            }
        }
    }

    private fun weeklySegments(
        config: QuickSetConfig.Weekly,
        span: HalfOpenDateSpan,
    ): List<QuickSetSegment> {
        val periodDays = ((config.endDayOfWeek.value - config.startDayOfWeek.value + 7) % 7 + 1).toLong()

        return buildList {
            var cursor = span.startDate.nextOrSame(config.startDayOfWeek)
            var current = cursor untilExclusive cursor.plusDays(periodDays)
            while ((current intersect span).length >= periodDays / 2) {
                add(QuickSetSegment(current, config.amount))
                cursor = cursor.plusDays(7)
                current = cursor untilExclusive cursor.plusDays(periodDays)
            }
        }
    }

    private fun totalAmountOrNull(segments: List<QuickSetSegment>): Long? {
        var total = 0L
        for (segment in segments) {
            if (total > Long.MAX_VALUE - segment.amount) return null
            total += segment.amount
        }
        return total
    }
}
