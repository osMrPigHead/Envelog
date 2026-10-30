package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.EntryEntity
import cn.osmrpighead.envelog.data.db.entity.EntryType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * 仅 debug 构建使用的演示数据。
 *
 * 数据以启动当月为基准生成，因此任意月份启动都能直接看到 Overview 与信封日历的内容。
 * 进程结束后内存仓库清空，下次启动会按新的日期重新生成。
 */
object DemoSeedData {
    private fun id(value: Long): UUID = UUID(0L, value)

    private val foodEnvelopeId = id(0xA001)
    private val transportEnvelopeId = id(0xA002)
    private val shoppingEnvelopeId = id(0xA003)
    private val householdEnvelopeId = id(0xA004)
    private val archivedEnvelopeId = id(0xA005)

    fun build(today: LocalDate, zone: ZoneId): EnvelogSeedData {
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)

        val envelopes = listOf(
            EnvelopeEntity(foodEnvelopeId, "餐饮", isBudgeted = true, archivedAt = false),
            EnvelopeEntity(transportEnvelopeId, "交通", isBudgeted = true, archivedAt = false),
            EnvelopeEntity(shoppingEnvelopeId, "购物", isBudgeted = false, archivedAt = false),
            EnvelopeEntity(householdEnvelopeId, "居住", isBudgeted = true, archivedAt = false),
            EnvelopeEntity(archivedEnvelopeId, "旧信封", isBudgeted = true, archivedAt = true),
        )

        val budgets = buildList {
            add(BudgetEntity(id(0xA101), foodEnvelopeId, monthStart, monthEnd, 300_000))
            add(BudgetEntity(id(0xA102), transportEnvelopeId, monthStart, monthEnd, 20_000))
            addAll(weeklyBudgets(householdEnvelopeId, monthStart, monthEnd, 50_000))
            add(BudgetEntity(id(0xA103), archivedEnvelopeId, monthStart, monthEnd, 50_000))
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

        val transactions = mutableListOf<EntryEntity>()
        val allocations = mutableListOf<ExpenditureAllocationEntity>()

        fun record(
            transactionId: UUID,
            date: LocalDate,
            amount: Long,
            shares: List<Pair<UUID, Long>>,
        ) {
            require(shares.sumOf { it.second } == amount) { "Allocation total must equal transaction amount" }
            transactions += EntryEntity(
                id = transactionId,
                type = EntryType.Expenditure,
                happenedAt = ZonedDateTime.of(date, LocalTime.NOON, zone),
                amount = amount,
            )
            shares.forEach { (envelopeId, share) ->
                allocations += ExpenditureAllocationEntity(
                    entryId = transactionId,
                    envelopeId = envelopeId,
                    amount = share,
                )
            }
        }

        record(id(0xB001), today, 12_000, listOf(foodEnvelopeId to 12_000))
        record(id(0xB002), today, 26_000, listOf(transportEnvelopeId to 26_000))
        record(id(0xB003), today, 30_000, listOf(shoppingEnvelopeId to 30_000))
        record(id(0xB004), today, 10_000, listOf(archivedEnvelopeId to 10_000))
        foodSecondDate?.let {
            record(id(0xB005), it, 8_000, listOf(foodEnvelopeId to 8_000))
        }
        splitDate?.let {
            record(
                id(0xB006),
                it,
                18_000,
                listOf(foodEnvelopeId to 13_000, shoppingEnvelopeId to 5_000),
            )
        }

        return EnvelogSeedData(
            envelopes = envelopes,
            budgets = budgets,
            transactions = transactions,
            allocations = allocations,
        )
    }

    private fun weeklyBudgets(
        envelopeId: UUID,
        start: LocalDate,
        endExclusive: LocalDate,
        amount: Long,
    ): List<BudgetEntity> = buildList {
        var budgetStart = start
        var index = 0
        while (budgetStart < endExclusive) {
            val budgetEnd = minOf(budgetStart.plusDays(7), endExclusive)
            add(
                BudgetEntity(
                    id = id(0xA201L + index),
                    envelopeId = envelopeId,
                    startDate = budgetStart,
                    endDate = budgetEnd,
                    amount = amount,
                ),
            )
            budgetStart = budgetEnd
            index++
        }
    }
}
