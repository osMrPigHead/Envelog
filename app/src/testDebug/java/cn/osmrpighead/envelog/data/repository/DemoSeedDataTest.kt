package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.DateRangeRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DemoSeedDataTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun clockAt(date: LocalDate): Clock = Clock.fixed(date.atStartOfDay(zone).toInstant(), zone)

    private fun dateRange(date: LocalDate): DateRange {
        val monthStart = date.withDayOfMonth(1)
        return DateRange(
            start = monthStart,
            end = monthStart.plusMonths(1),
            rule = DateRangeRule.StartInRange,
        )
    }

    private fun buildSeed(date: LocalDate): EnvelogSeedData =
        DemoSeedData.build(LocalDate.now(clockAt(date)), zone)

    private fun repositoryOf(seed: EnvelogSeedData): InMemoryEnvelogRepository =
        InMemoryEnvelogRepository(
            envelopes = seed.envelopes,
            budgets = seed.budgets,
            transactions = seed.transactions,
            allocations = seed.allocations,
        )

    @Test
    fun demoSeedData_hasConsistentReferencesAndPeriods() {
        val today = LocalDate.of(2026, 10, 25)
        val seed = buildSeed(today)

        val allIds = seed.envelopes.map { it.id } +
            seed.budgets.map { it.id } +
            seed.transactions.map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)

        assertTrue(seed.budgets.all { budget -> seed.envelopes.any { it.id == budget.envelopeId } })
        assertTrue(seed.allocations.all { allocation ->
            seed.transactions.any { it.id == allocation.transactionId } &&
                seed.envelopes.any { it.id == allocation.envelopeId }
        })

        seed.budgets.groupBy { it.envelopeId }.values.forEach { budgets ->
            budgets.sortedBy { it.startDate }.zipWithNext().forEach { (left, right) ->
                assertTrue(left.endDate <= right.startDate)
            }
        }

        val allocationTotals = seed.allocations.groupBy { it.transactionId }
            .mapValues { (_, allocations) -> allocations.sumOf { it.amount } }
        seed.transactions.forEach { transaction ->
            assertEquals(transaction.amount, allocationTotals[transaction.id] ?: 0L)
        }

        val month = YearMonth.from(today)
        val monthStart = today.withDayOfMonth(1)
        val monthEnd = monthStart.plusMonths(1)
        assertTrue(seed.transactions.all { YearMonth.from(it.dateTime.toLocalDate()) == month })
        assertTrue(seed.transactions.all { !it.dateTime.toLocalDate().isAfter(today) })
        assertTrue(seed.budgets.all { it.startDate >= monthStart && it.endDate <= monthEnd })
    }

    @Test
    fun populatedRepository_providesDataForOverviewAndEnvelopeSheet() = runTest {
        val today = LocalDate.of(2026, 10, 25)
        val seed = buildSeed(today)
        val repository = repositoryOf(seed)
        val range = dateRange(today)

        val active = repository.observeActiveEnvelopes().first()
        assertEquals(listOf("餐饮", "交通", "购物", "居住"), active.map { it.name })
        assertFalse(active.any { it.archived })

        val extra = repository.observeActiveEnvelopesWithExtra(range).first()
            .associateBy { it.envelope.name }
        assertEquals(4, extra.size)
        assertFalse(extra.containsKey("旧信封"))

        val food = extra.getValue("餐饮")
        assertEquals(300_000, food.budgets.sumOf { it.amount })
        assertEquals(33_000, food.allocations.sumOf { it.allocation.amount })

        val transport = extra.getValue("交通")
        assertTrue(
            transport.allocations.sumOf { it.allocation.amount } >
                transport.budgets.sumOf { it.amount },
        )

        val shopping = extra.getValue("购物")
        assertTrue(shopping.budgets.isEmpty())
        assertEquals(35_000, shopping.allocations.sumOf { it.allocation.amount })

        val household = extra.getValue("居住")
        assertTrue(household.budgets.size >= 4)
        assertTrue(household.budgets.any { today >= it.startDate && today < it.endDate })
        assertTrue(household.allocations.isEmpty())

        assertEquals(104_000, repository.observeTransactions(range).first().sumOf { it.amount })
    }

    @Test
    fun demoSeedData_remainsUsableOnFirstDayOfMonth() = runTest {
        val today = LocalDate.of(2026, 10, 1)
        val seed = buildSeed(today)
        val repository = repositoryOf(seed)
        val range = dateRange(today)

        assertTrue(seed.transactions.all { !it.dateTime.toLocalDate().isBefore(today) })
        assertEquals(78_000, repository.observeTransactions(range).first().sumOf { it.amount })

        val extra = repository.observeActiveEnvelopesWithExtra(range).first()
            .associateBy { it.envelope.name }
        listOf("餐饮", "交通", "购物").forEach { name ->
            assertTrue(extra.getValue(name).allocations.isNotEmpty())
        }
    }
}
