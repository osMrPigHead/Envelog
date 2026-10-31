package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * 仅 debug 构建使用的数据库演示数据。
 *
 * 数据以启动当月为基准生成，因此任意月份启动都能直接看到 Overview 的内容。
 */
data class DemoSeed(
    val envelopes: List<EnvelopeEntity>,
    val budgets: List<BudgetEntity>,
    val expenditures: List<ExpenditureEntity>,
    val allocations: List<ExpenditureAllocationEntity>,
)

object DemoSeedData {
    private fun envelopeId(value: Long): EnvelopeId = EnvelopeId(UUID(0L, value))
    private fun budgetId(value: Long): BudgetId = BudgetId(UUID(0L, value))
    private fun expenditureId(value: Long): ExpenditureId = ExpenditureId(UUID(0L, value))

    private val foodEnvelopeId = envelopeId(0xA001)
    private val transportEnvelopeId = envelopeId(0xA002)
    private val shoppingEnvelopeId = envelopeId(0xA003)
    private val householdEnvelopeId = envelopeId(0xA004)
    private val archivedEnvelopeId = envelopeId(0xA005)

    fun build(today: LocalDate, zone: ZoneId): DemoSeed {
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)
        val month = YearMonth.from(today)
        val archivedAt = monthStart.atStartOfDay(zone).toInstant()

        val envelopes = listOf(
            EnvelopeEntity(foodEnvelopeId, "餐饮", isBudgeted = true, archivedAt = null),
            EnvelopeEntity(transportEnvelopeId, "交通", isBudgeted = true, archivedAt = null),
            EnvelopeEntity(shoppingEnvelopeId, "购物", isBudgeted = false, archivedAt = null),
            EnvelopeEntity(householdEnvelopeId, "居住", isBudgeted = true, archivedAt = null),
            EnvelopeEntity(archivedEnvelopeId, "旧信封", isBudgeted = true, archivedAt = archivedAt),
        )

        val budgets = buildList {
            add(monthlyBudget(0xA101, foodEnvelopeId, monthStart, monthEnd, month, 300_000))
            add(monthlyBudget(0xA102, transportEnvelopeId, monthStart, monthEnd, month, 20_000))
            addAll(weeklyBudgets(householdEnvelopeId, monthStart, monthEnd, month, 50_000))
            add(monthlyBudget(0xA103, archivedEnvelopeId, monthStart, monthEnd, month, 50_000))
        }

        val previousDays = buildList {
            var date = today.minusDays(1)
            while (date >= monthStart) {
                add(date)
                date = date.minusDays(1)
            }
        }
        val foodSecondDate = previousDays.getOrNull(0)
        val splitDate = previousDays.getOrNull(1)

        val expenditures = mutableListOf<ExpenditureEntity>()
        val allocations = mutableListOf<ExpenditureAllocationEntity>()

        fun record(
            idValue: Long,
            date: LocalDate,
            shares: List<Pair<EnvelopeId, Long>>,
        ) {
            val transactionId = expenditureId(idValue)
            expenditures += ExpenditureEntity(
                id = transactionId,
                happenedAt = date.atTime(LocalTime.NOON).atZone(zone).toInstant(),
                happenedDate = date,
                happenedZone = zone,
                deletedAt = null,
            )
            shares.forEach { (envelopeId, share) ->
                allocations += ExpenditureAllocationEntity(
                    expenditureId = transactionId,
                    envelopeId = envelopeId,
                    amount = share,
                )
            }
        }

        record(0xB001, today, listOf(foodEnvelopeId to 12_000))
        record(0xB002, today, listOf(transportEnvelopeId to 26_000))
        record(0xB003, today, listOf(shoppingEnvelopeId to 30_000))
        record(0xB004, today, listOf(archivedEnvelopeId to 10_000))
        foodSecondDate?.let {
            record(0xB005, it, listOf(foodEnvelopeId to 8_000))
        }
        splitDate?.let {
            record(
                0xB006,
                it,
                listOf(foodEnvelopeId to 13_000, shoppingEnvelopeId to 5_000),
            )
        }

        return DemoSeed(
            envelopes = envelopes,
            budgets = budgets,
            expenditures = expenditures,
            allocations = allocations,
        )
    }

    private fun monthlyBudget(
        idValue: Long,
        envelopeId: EnvelopeId,
        start: LocalDate,
        endExclusive: LocalDate,
        month: YearMonth,
        amount: Long,
    ): BudgetEntity = BudgetEntity(
        id = budgetId(idValue),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, endExclusive),
        attributedMonth = month,
        amount = amount,
        deletedAt = null,
    )

    private fun weeklyBudgets(
        envelopeId: EnvelopeId,
        start: LocalDate,
        endExclusive: LocalDate,
        month: YearMonth,
        amount: Long,
    ): List<BudgetEntity> = buildList {
        var budgetStart = start
        var index = 0
        while (budgetStart < endExclusive) {
            val budgetEnd = minOf(budgetStart.plusDays(7), endExclusive)
            add(
                monthlyBudget(
                    idValue = 0xA201L + index,
                    envelopeId = envelopeId,
                    start = budgetStart,
                    endExclusive = budgetEnd,
                    month = month,
                    amount = amount,
                ),
            )
            budgetStart = budgetEnd
            index++
        }
    }
}
