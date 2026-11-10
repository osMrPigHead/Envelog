package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.testing.RoomTestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class EnvelopeUiStateTest {
    private val envelopeId = RoomTestData.envelopeA
    private val month = RoomTestData.month

    private fun date(day: Int): LocalDate = LocalDate.of(2026, 10, day)

    private fun budget(value: Long, amount: Long = 100_000): Budget = Budget(
        id = BudgetId(RoomTestData.uuid(value)),
        envelopeId = envelopeId,
        dateSpan = month.toHalfOpenDateSpan(),
        attributedMonth = month,
        amount = amount,
    )

    private fun success(
        budgets: Collection<Budget>,
        allocations: List<DailyAllocationSummary>,
    ): EnvelopeUiState.Success = EnvelopeUiState.Success(
        envelope = Envelope(envelopeId, "餐饮", true, false),
        yearMonth = month,
        budgets = budgets.associateBy { it.id },
        amountFieldId = Any(),
        allocations = allocations,
        activeBudget = null,
        activeIsNew = false,
    )

    @Test
    fun success_groupsAllocationsByHappenedDate() {
        val first = DailyAllocationSummary(envelopeId, date(5), 7_000)
        val second = DailyAllocationSummary(envelopeId, date(31), 5_000)

        val state = success(emptyList(), listOf(first, second))

        assertEquals(2, state.allocationsByDate.size)
        assertEquals(first, state.allocationsByDate[date(5)])
        assertEquals(second, state.allocationsByDate[date(31)])
    }

    @Test
    fun success_buildsMondayBasedCalendarWithBudgetMapping() {
        val wholeMonth = budget(0xD1)
        val allocation = DailyAllocationSummary(envelopeId, date(5), 7_000)

        val state = success(listOf(wholeMonth), listOf(allocation))

        assertEquals(5, state.calendar.weeks.size)
        assertEquals(31, state.calendar.dates.size)
        assertEquals(wholeMonth.id, state.calendar.dates[date(5)]!!.budgetId)
        assertEquals(allocation, state.calendar.dates[date(5)]!!.allocationSummary)
        assertNull(state.calendar.dates[date(1)]!!.allocationSummary)
    }
}
