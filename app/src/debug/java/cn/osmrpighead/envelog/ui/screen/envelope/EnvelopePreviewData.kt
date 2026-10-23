package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import cn.osmrpighead.envelog.util.date.DatePeriod
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

internal object EnvelopePreviewData {

    val period: DatePeriod = LocalDate.of(2026, 9, 1) to LocalDate.of(2026, 10, 1)

    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

    val allocations: Map<LocalDate, Long> = buildMap {
        for (date in days()) {
            if (date.dayOfMonth % 10 != 0) put(date, 6_566L)
        }
    }

    private val envelopeId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")

    val mixedBudgets: List<Budget> = listOf(
        budgetOf(0, LocalDate.of(2026, 9, 3) to LocalDate.of(2026, 9, 9), 30_000),
        budgetOf(1, LocalDate.of(2026, 9, 10) to LocalDate.of(2026, 9, 13), 12_000),
        budgetOf(2, LocalDate.of(2026, 9, 16) to LocalDate.of(2026, 9, 30), 50_000),
    )

    val dailyBudgets: List<Budget> = days().mapIndexed { index, date ->
        budgetOf(index, date to date.plusDays(1), 8_000)
    }

    val workdayBudgets: List<Budget> = days()
        .filter { it.dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY }
        .mapIndexed { index, date -> budgetOf(index, date to date.plusDays(1), 9_000) }

    val weeklyWorkdayBudgets: List<Budget> = (0 until 5).map { index ->
        val startDate = LocalDate.of(2026, 8, 31).plusWeeks(index.toLong())
        budgetOf(index, startDate to startDate.plusDays(5), 45_000)
    }

    val mixedCalendar: BudgetCalendarModel = calendarOf(mixedBudgets)

    val dailyCalendar: BudgetCalendarModel = calendarOf(dailyBudgets)

    val workdayCalendar: BudgetCalendarModel = calendarOf(workdayBudgets)

    val weeklyWorkdayCalendar: BudgetCalendarModel = calendarOf(weeklyWorkdayBudgets)

    private fun days(): List<LocalDate> = buildList {
        var date = period.first
        while (date < period.second) {
            add(date)
            date = date.plusDays(1)
        }
    }

    private fun budgetOf(index: Int, dates: DatePeriod, amount: Long): Budget = Budget(
        id = UUID(0L, (index + 1).toLong()),
        envelopeId = envelopeId,
        startDate = dates.first,
        endDate = dates.second,
        amount = amount,
    )

    private fun calendarOf(budgets: List<Budget>): BudgetCalendarModel =
        BudgetCalendarModel(allocations = allocations, budgets = budgets)
}
