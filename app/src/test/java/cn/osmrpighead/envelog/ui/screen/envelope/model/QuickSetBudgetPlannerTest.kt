package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.next
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.testing.RoomTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class QuickSetBudgetPlannerTest {
    private val envelopeId = RoomTestData.envelopeA
    private val month = RoomTestData.month
    private val today = RoomTestData.today

    private fun date(month: Int, day: Int): LocalDate = LocalDate.of(2026, month, day)

    private fun budget(
        value: Long,
        start: LocalDate,
        end: LocalDate,
        envelopeId: EnvelopeId = this.envelopeId,
        amount: Long = 100_000,
    ): Budget = Budget(
        id = RoomTestData.budgetId(value),
        envelopeId = envelopeId,
        dateSpan = start untilExclusive end,
        attributedMonth = YearMonth.from(start),
        amount = amount,
    )

    private fun idsFrom(value: Long): () -> BudgetId {
        var next = value
        return { RoomTestData.budgetId(next++) }
    }

    @Test
    fun targetSpan_dailyInCurrentMonth_startsToday() {
        val span = QuickSetBudgetPlanner.targetSpan(QuickSetMode.Daily, month, today)

        assertEquals(date(10, 20), span!!.startDate)
        assertEquals(date(11, 1), span.endDate)
    }

    @Test
    fun targetSpan_dailyInFutureMonth_startsAtMonthStart() {
        val span = QuickSetBudgetPlanner.targetSpan(QuickSetMode.Daily, month.next, today)

        assertEquals(date(11, 1), span!!.startDate)
        assertEquals(date(12, 1), span.endDate)
    }

    @Test
    fun targetSpan_inPastMonth_isNull() {
        assertNull(QuickSetBudgetPlanner.targetSpan(QuickSetMode.Daily, month.minusMonths(1), today))
        assertNull(QuickSetBudgetPlanner.targetSpan(QuickSetMode.MonthlyTotal, month.minusMonths(1), today))
    }

    @Test
    fun targetSpan_totalAlwaysCoversWholeMonth() {
        val current = QuickSetBudgetPlanner.targetSpan(QuickSetMode.MonthlyTotal, month, today)
        val future = QuickSetBudgetPlanner.targetSpan(QuickSetMode.MonthlyTotal, month.next, today)

        assertEquals(date(10, 1), current!!.startDate)
        assertEquals(date(11, 1), current.endDate)
        assertEquals(date(11, 1), future!!.startDate)
        assertEquals(date(12, 1), future.endDate)
    }

    @Test
    fun plan_daily_createsOneBudgetPerDay() {
        val plan = QuickSetBudgetPlanner.plan(
            mode = QuickSetMode.Daily,
            amount = 5_000,
            yearMonth = month,
            today = today,
            envelopeId = envelopeId,
            existing = emptyList(),
            newId = idsFrom(0x100),
        )!!

        assertEquals(12, plan.budgets.size)
        assertEquals(60_000, plan.totalAmount)
        assertEquals(0, plan.conflicts.size)
        assertEquals(date(10, 20), plan.targetSpan.startDate)
        assertEquals(date(11, 1), plan.targetSpan.endDate)

        for ((index, budget) in plan.budgets.withIndex()) {
            val expectedStart = date(10, 20).plusDays(index.toLong())
            assertEquals(expectedStart, budget.dateSpan.startDate)
            assertEquals(expectedStart.plusDays(1), budget.dateSpan.endDate)
            assertEquals(month, budget.attributedMonth)
            assertEquals(5_000, budget.amount)
            assertEquals(envelopeId, budget.envelopeId)
        }
        assertEquals(plan.budgets.size, plan.budgets.map { it.id }.toSet().size)
    }

    @Test
    fun plan_total_createsWholeMonthBudget() {
        val plan = QuickSetBudgetPlanner.plan(
            mode = QuickSetMode.MonthlyTotal,
            amount = 150_000,
            yearMonth = month,
            today = today,
            envelopeId = envelopeId,
            existing = emptyList(),
            newId = idsFrom(0x100),
        )!!

        assertEquals(1, plan.budgets.size)
        assertEquals(150_000, plan.totalAmount)
        assertEquals(date(10, 1), plan.budgets.single().dateSpan.startDate)
        assertEquals(date(11, 1), plan.budgets.single().dateSpan.endDate)
        assertEquals(month, plan.budgets.single().attributedMonth)
        assertEquals(150_000, plan.budgets.single().amount)
    }

    @Test
    fun plan_marksIntersectingBudgetsAsConflicts() {
        val adjacent = budget(0xD1, date(10, 18), date(10, 20))
        val overlapping = budget(0xD2, date(10, 19), date(10, 22))
        val inside = budget(0xD3, date(10, 25), date(10, 26))

        val daily = QuickSetBudgetPlanner.plan(
            mode = QuickSetMode.Daily,
            amount = 5_000,
            yearMonth = month,
            today = today,
            envelopeId = envelopeId,
            existing = listOf(adjacent, overlapping, inside),
            newId = idsFrom(0x100),
        )!!
        assertEquals(setOf(overlapping, inside), daily.conflicts.toSet())

        val total = QuickSetBudgetPlanner.plan(
            mode = QuickSetMode.MonthlyTotal,
            amount = 150_000,
            yearMonth = month,
            today = today,
            envelopeId = envelopeId,
            existing = listOf(adjacent, overlapping, inside),
            newId = idsFrom(0x100),
        )!!
        assertEquals(setOf(adjacent, overlapping, inside), total.conflicts.toSet())
    }

    @Test
    fun plan_ignoresAdjacentAndOtherEnvelopeBudgets() {
        val adjacent = budget(0xD1, date(10, 1), date(10, 20))
        val otherEnvelope = budget(0xD2, date(10, 21), date(10, 22), envelopeId = RoomTestData.envelopeB)

        val plan = QuickSetBudgetPlanner.plan(
            mode = QuickSetMode.Daily,
            amount = 5_000,
            yearMonth = month,
            today = today,
            envelopeId = envelopeId,
            existing = listOf(adjacent, otherEnvelope),
            newId = idsFrom(0x100),
        )!!

        assertTrue(plan.conflicts.isEmpty())
    }

    @Test
    fun plan_withNonPositiveAmount_isNull() {
        assertNull(
            QuickSetBudgetPlanner.plan(
                mode = QuickSetMode.Daily,
                amount = 0,
                yearMonth = month,
                today = today,
                envelopeId = envelopeId,
                existing = emptyList(),
            ),
        )
        assertNull(
            QuickSetBudgetPlanner.plan(
                mode = QuickSetMode.MonthlyTotal,
                amount = -1,
                yearMonth = month,
                today = today,
                envelopeId = envelopeId,
                existing = emptyList(),
            ),
        )
    }

    @Test
    fun plan_inPastMonth_isNull() {
        assertNull(
            QuickSetBudgetPlanner.plan(
                mode = QuickSetMode.Daily,
                amount = 5_000,
                yearMonth = month.minusMonths(1),
                today = today,
                envelopeId = envelopeId,
                existing = emptyList(),
            ),
        )
    }

    @Test
    fun totalAmount_returnsNullOnOverflow() {
        val span = QuickSetBudgetPlanner.targetSpan(QuickSetMode.Daily, month, today)!!

        assertNull(QuickSetBudgetPlanner.totalAmount(QuickSetMode.Daily, Long.MAX_VALUE, span))
        assertEquals(Long.MAX_VALUE, QuickSetBudgetPlanner.totalAmount(QuickSetMode.MonthlyTotal, Long.MAX_VALUE, span))
    }

    @Test
    fun totalAmount_returnsNullForEmptySpan() {
        assertNull(QuickSetBudgetPlanner.totalAmount(QuickSetMode.Daily, 1, date(10, 1) untilExclusive date(10, 1)))
    }
}
