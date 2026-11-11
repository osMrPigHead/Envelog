package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.testing.RoomTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class BudgetCalendarModelTest {
    private val envelopeId = RoomTestData.envelopeA
    private val month = RoomTestData.month

    private fun date(day: Int): LocalDate = LocalDate.of(2026, 10, day)

    private fun budget(
        value: Long,
        start: LocalDate,
        end: LocalDate,
        amount: Long = 100_000,
    ): Budget = Budget(
        id = BudgetId(RoomTestData.uuid(value)),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, end),
        attributedMonth = month,
        amount = amount,
    )

    private fun model(
        budgets: Collection<Budget> = emptyList(),
        allocations: Map<LocalDate, DailyAllocationSummary> = emptyMap(),
    ): EnvelopeCalendarModel = EnvelopeCalendarModel.of(
        span = month.toHalfOpenDateSpan(),
        firstDayOfWeek = DayOfWeek.MONDAY,
        budgets = budgets,
        allocations = allocations,
    )

    @Test
    fun of_withoutBudgetsOrAllocations_buildsFiveWeeks() {
        val result = model()

        assertEquals(5, result.weeks.size)
        assertEquals(3L, result.weeks[0].before)
        assertEquals(0L, result.weeks[0].after)
        for (index in 1..3) {
            assertEquals(0L, result.weeks[index].before)
            assertEquals(0L, result.weeks[index].after)
        }
        assertEquals(0L, result.weeks[4].before)
        assertEquals(1L, result.weeks[4].after)
        result.weeks.forEach { week ->
            assertEquals(listOf(BudgetDisplay.Spacer(7)), week.budgets)
        }
        assertEquals(31, result.dates.size)
        result.dates.values.forEach { dateModel ->
            assertNull(dateModel.allocationSummary)
            assertNull(dateModel.budgetId)
        }
    }

    @Test
    fun of_withWholeMonthBudget_marksLeftExternalAndRightBoundWeeks() {
        val wholeMonth = budget(0xD1, date(1), LocalDate.of(2026, 11, 1), amount = 100_000)

        val result = model(budgets = listOf(wholeMonth))

        assertEquals(
            listOf(BudgetDisplay.Spacer(3), BudgetDisplay.LeftBound(4, 100_000, wholeMonth.id)),
            result.weeks[0].budgets,
        )
        for (index in 1..3) {
            assertEquals(
                listOf(BudgetDisplay.External(7, 100_000, wholeMonth.id)),
                result.weeks[index].budgets,
            )
        }
        assertEquals(
            listOf(BudgetDisplay.RightBound(6, 100_000, wholeMonth.id), BudgetDisplay.Spacer(1)),
            result.weeks[4].budgets,
        )
    }

    @Test
    fun of_withBudgetInsideWeek_surroundsWithSpacers() {
        val inner = budget(0xD2, date(7), date(9), amount = 5_000)

        val result = model(budgets = listOf(inner))

        assertEquals(
            listOf(
                BudgetDisplay.Spacer(2),
                BudgetDisplay.Internal(2, 5_000, inner.id),
                BudgetDisplay.Spacer(3),
            ),
            result.weeks[1].budgets,
        )
        assertEquals(listOf(BudgetDisplay.Spacer(7)), result.weeks[0].budgets)
        assertNull(result.dates[date(6)]!!.budgetId)
        assertEquals(inner.id, result.dates[date(7)]!!.budgetId)
        assertEquals(inner.id, result.dates[date(8)]!!.budgetId)
        assertNull(result.dates[date(9)]!!.budgetId)
    }

    @Test
    fun of_withBudgetCrossingWeekBoundary_usesLeftAndRightBound() {
        val crossing = budget(0xD3, date(3), date(7), amount = 8_000)

        val result = model(budgets = listOf(crossing))

        assertEquals(
            listOf(BudgetDisplay.Spacer(5), BudgetDisplay.LeftBound(2, 8_000, crossing.id)),
            result.weeks[0].budgets,
        )
        assertEquals(
            listOf(BudgetDisplay.RightBound(2, 8_000, crossing.id), BudgetDisplay.Spacer(5)),
            result.weeks[1].budgets,
        )
    }

    @Test
    fun of_withAdjacentBudgets_emitsNoGap() {
        val first = budget(0xD4, date(1), date(8), amount = 100)
        val second = budget(0xD5, date(8), date(15), amount = 200)

        val result = model(budgets = listOf(first, second))

        assertEquals(
            listOf(
                BudgetDisplay.RightBound(3, 100, first.id),
                BudgetDisplay.LeftBound(4, 200, second.id),
            ),
            result.weeks[1].budgets,
        )
    }

    @Test
    fun of_withGapBetweenBudgets_emitsSpacer() {
        val first = budget(0xD6, date(5), date(7), amount = 100)
        val second = budget(0xD7, date(9), date(11), amount = 200)

        val result = model(budgets = listOf(first, second))

        assertEquals(
            listOf(
                BudgetDisplay.Internal(2, 100, first.id),
                BudgetDisplay.Spacer(2),
                BudgetDisplay.Internal(2, 200, second.id),
                BudgetDisplay.Spacer(1),
            ),
            result.weeks[1].budgets,
        )
    }

    @Test
    fun of_withBudgetOutsideSpan_ignoresIt() {
        val outside = budget(0xD8, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))

        val result = model(budgets = listOf(outside))

        result.weeks.forEach { week ->
            assertEquals(listOf(BudgetDisplay.Spacer(7)), week.budgets)
        }
        result.dates.values.forEach { dateModel ->
            assertNull(dateModel.budgetId)
        }
    }

    @Test
    fun of_mapsAllocationsToDates() {
        val allocationOnFifth = DailyAllocationSummary(envelopeId, date(5), 7_000)
        val allocationOnLastDay = DailyAllocationSummary(envelopeId, date(31), 5_000)

        val result = model(
            allocations = mapOf(
                date(5) to allocationOnFifth,
                date(31) to allocationOnLastDay,
            ),
        )

        assertEquals(31, result.dates.size)
        assertEquals(allocationOnFifth, result.dates[date(5)]!!.allocationSummary)
        assertEquals(allocationOnLastDay, result.dates[date(31)]!!.allocationSummary)
        assertNull(result.dates[date(6)]!!.allocationSummary)
        assertNull(result.dates[date(5)]!!.budgetId)
    }
}
