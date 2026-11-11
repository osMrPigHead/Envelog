package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.testing.RoomTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class QuickSetBudgetPlannerTest {
    private val envelopeId = RoomTestData.envelopeA
    private val month = RoomTestData.month
    private val wholeMonth = ClosedDateSpan(date(10, 1), date(10, 31))

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
    fun schedule_dailyOneDay_createsOneSegmentPerDay() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.Daily(intervalDays = 1, amount = 5_000),
            selection = wholeMonth,
            yearMonth = month,
        )!!

        assertEquals(31, schedule.segments.size)
        assertEquals(155_000, schedule.totalAmount)
        assertEquals(date(10, 1), schedule.segments.first().span.startDate)
        assertEquals(date(10, 2), schedule.segments.first().span.endDate)
        assertEquals(date(10, 31), schedule.segments.last().span.startDate)
        assertEquals(date(11, 1), schedule.segments.last().span.endDate)
        assertTrue(schedule.segments.all { it.amount == 5_000L })
    }

    @Test
    fun schedule_dailyMultipleDays_dropsIncompleteLastPeriod() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.Daily(intervalDays = 2, amount = 10_000),
            selection = wholeMonth,
            yearMonth = month,
        )!!

        assertEquals(15, schedule.segments.size)
        assertEquals(150_000, schedule.totalAmount)
        assertEquals(date(10, 29), schedule.segments.last().span.startDate)
        assertEquals(date(10, 31), schedule.segments.last().span.endDate)
        assertEquals(30, schedule.segments.sumOf { it.span.length })
    }

    @Test
    fun schedule_dailyPeriodLongerThanSelection_isNull() {
        assertNull(
            QuickSetBudgetPlanner.schedule(
                config = QuickSetConfig.Daily(intervalDays = 7, amount = 10_000),
                selection = ClosedDateSpan(date(10, 1), date(10, 4)),
                yearMonth = month,
            ),
        )
    }

    @Test
    fun schedule_weeklyWholeWeek_anchorsAtFirstMondayAndDropsIncompleteTail() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.Weekly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 100_000),
            selection = wholeMonth,
            yearMonth = month,
        )!!

        assertEquals(3, schedule.segments.size)
        assertEquals(300_000, schedule.totalAmount)
        assertEquals(date(10, 5), schedule.segments.first().span.startDate)
        assertEquals(date(10, 12), schedule.segments.first().span.endDate)
        assertEquals(date(10, 19), schedule.segments.last().span.startDate)
        assertEquals(date(10, 26), schedule.segments.last().span.endDate)
    }

    @Test
    fun schedule_weeklyCrossWeek_supportsFridayToTuesday() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.Weekly(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY, 100_000),
            selection = wholeMonth,
            yearMonth = month,
        )!!

        assertEquals(4, schedule.segments.size)
        assertEquals(date(10, 2), schedule.segments.first().span.startDate)
        assertEquals(date(10, 7), schedule.segments.first().span.endDate)
        assertEquals(date(10, 9), schedule.segments[1].span.startDate)
        assertEquals(date(10, 14), schedule.segments[1].span.endDate)
        assertEquals(5, schedule.segments.first().span.length)
    }

    @Test
    fun schedule_weeklyDropsIncompleteLastPeriod() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.Weekly(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY, 100_000),
            selection = ClosedDateSpan(date(10, 1), date(10, 9)),
            yearMonth = month,
        )!!

        assertEquals(1, schedule.segments.size)
        assertEquals(date(10, 2), schedule.segments.single().span.startDate)
        assertEquals(date(10, 7), schedule.segments.single().span.endDate)
        assertEquals(100_000, schedule.totalAmount)
    }

    @Test
    fun schedule_monthly_ignoresSelectionAndCoversWholeMonth() {
        val schedule = QuickSetBudgetPlanner.schedule(
            config = QuickSetConfig.MonthlyTotal(150_000),
            selection = ClosedDateSpan(date(10, 5), date(10, 10)),
            yearMonth = month,
        )!!

        assertEquals(1, schedule.segments.size)
        assertEquals(date(10, 1), schedule.segments.single().span.startDate)
        assertEquals(date(11, 1), schedule.segments.single().span.endDate)
        assertEquals(150_000, schedule.totalAmount)
    }

    @Test
    fun plan_createsBudgetsWithDisplayedMonthAttribution() {
        val plan = QuickSetBudgetPlanner.plan(
            config = QuickSetConfig.Daily(intervalDays = 1, amount = 5_000),
            selection = wholeMonth,
            yearMonth = month,
            envelopeId = envelopeId,
            existing = emptyList(),
            newId = idsFrom(0x100),
        )!!

        assertEquals(31, plan.budgets.size)
        assertEquals(155_000, plan.totalAmount)
        assertEquals(0, plan.conflicts.size)
        assertEquals(date(10, 1), plan.targetSpan.startDate)
        assertEquals(date(11, 1), plan.targetSpan.endDate)
        for (budget in plan.budgets) {
            assertEquals(month, budget.attributedMonth)
            assertEquals(envelopeId, budget.envelopeId)
            assertEquals(5_000, budget.amount)
        }
        assertEquals(plan.budgets.size, plan.budgets.map { it.id }.toSet().size)
    }

    @Test
    fun plan_marksOnlyBudgetsIntersectingGeneratedSegmentsAsConflicts() {
        val inGap = budget(0xD1, date(10, 7), date(10, 9))
        val overlappingFirst = budget(0xD2, date(10, 3), date(10, 4))
        val overlappingLater = budget(0xD3, date(10, 20), date(10, 21))
        val inAnotherGap = budget(0xD4, date(10, 14), date(10, 16))

        val plan = QuickSetBudgetPlanner.plan(
            config = QuickSetConfig.Weekly(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY, 100_000),
            selection = wholeMonth,
            yearMonth = month,
            envelopeId = envelopeId,
            existing = listOf(inGap, overlappingFirst, overlappingLater, inAnotherGap),
            newId = idsFrom(0x100),
        )!!

        assertEquals(setOf(overlappingFirst, overlappingLater), plan.conflicts.toSet())
    }

    @Test
    fun plan_ignoresOtherEnvelopeBudgets() {
        val otherEnvelope = budget(0xD1, date(10, 2), date(10, 10), envelopeId = RoomTestData.envelopeB)

        val plan = QuickSetBudgetPlanner.plan(
            config = QuickSetConfig.Daily(intervalDays = 1, amount = 5_000),
            selection = wholeMonth,
            yearMonth = month,
            envelopeId = envelopeId,
            existing = listOf(otherEnvelope),
            newId = idsFrom(0x100),
        )!!

        assertTrue(plan.conflicts.isEmpty())
    }

    @Test
    fun schedule_invalidAmountOrInterval_isNull() {
        assertNull(
            QuickSetBudgetPlanner.schedule(
                QuickSetConfig.Daily(intervalDays = 1, amount = 0),
                wholeMonth,
                month,
            ),
        )
        assertNull(
            QuickSetBudgetPlanner.schedule(
                QuickSetConfig.Daily(intervalDays = 0, amount = 5_000),
                wholeMonth,
                month,
            ),
        )
        assertNull(
            QuickSetBudgetPlanner.schedule(
                QuickSetConfig.Weekly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 0),
                wholeMonth,
                month,
            ),
        )
        assertNull(
            QuickSetBudgetPlanner.schedule(
                QuickSetConfig.MonthlyTotal(0),
                wholeMonth,
                month,
            ),
        )
    }

    @Test
    fun schedule_selectionOutsideMonth_isNull() {
        assertNull(
            QuickSetBudgetPlanner.schedule(
                config = QuickSetConfig.Daily(intervalDays = 1, amount = 5_000),
                selection = ClosedDateSpan(date(9, 1), date(9, 30)),
                yearMonth = month,
            ),
        )
    }

    @Test
    fun schedule_overflow_isNull() {
        assertNull(
            QuickSetBudgetPlanner.schedule(
                config = QuickSetConfig.Daily(intervalDays = 1, amount = Long.MAX_VALUE),
                selection = wholeMonth,
                yearMonth = month,
            ),
        )
    }
}
