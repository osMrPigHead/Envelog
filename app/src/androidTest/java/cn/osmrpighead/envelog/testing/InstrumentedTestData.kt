package cn.osmrpighead.envelog.testing

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.number.ratioOf
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.ui.screen.overview.model.SummaryItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * instrumented 测试共用的固定基线与查询模型工厂。
 */
object InstrumentedTestData {
    val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    val month: YearMonth = YearMonth.of(2026, 10)
    val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)
    val today: LocalDate = LocalDate.of(2026, 10, 20)

    fun envelopeId(value: Long): EnvelopeId = EnvelopeId(UUID(0L, value))

    fun budgetId(value: Long): BudgetId = BudgetId(UUID(0L, value))

    val envelopeA: EnvelopeId = envelopeId(0xA1)
    val envelopeB: EnvelopeId = envelopeId(0xA2)

    val summaryItems: List<SummaryItem> = listOf(
        SummaryItem.Remaining(cents = 150_000, usedProportion = 350_000L.ratioOf(500_000L)),
        SummaryItem.Expenses(cents = 350_000),
        SummaryItem.Balance(cents = -350_000),
    )

    fun budgetedEnvelope(
        id: EnvelopeId = envelopeA,
        name: String = "餐饮",
        canDelete: Boolean = true,
    ): EnvelopeSummary.Budgeted = EnvelopeSummary.Budgeted(
        id = id,
        name = name,
        allocated = 200_000,
        currency = CurrencyType.CNY,
        archived = false,
        canDelete = canDelete,
        totalBudget = 500_000,
        currentBudget = 180_000,
        currentSpan = HalfOpenDateSpan(monthStart, monthEnd),
        currentSpanAllocated = 140_000,
        priorBudget = 150_000,
        priorAllocated = 145_000,
    )

    fun nonBudgetedEnvelope(
        id: EnvelopeId = envelopeB,
        name: String = "购物",
        canDelete: Boolean = true,
    ): EnvelopeSummary.NonBudget = EnvelopeSummary.NonBudget(
        id = id,
        name = name,
        allocated = 30_000,
        currency = CurrencyType.CNY,
        archived = false,
        canDelete = canDelete,
    )

    fun budget(
        value: Long,
        envelopeId: EnvelopeId = envelopeA,
        amount: Long = 100_000,
    ): Budget = Budget(
        id = budgetId(value),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(monthStart, monthEnd),
        attributedMonth = month,
        amount = amount,
    )
}
