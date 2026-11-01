package cn.osmrpighead.envelog.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class DemoSeedDataTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private fun buildSeed(date: LocalDate): DemoSeed = DemoSeedData.build(date, zone)

    @Test
    fun demoSeedData_hasConsistentReferencesAndPeriods() {
        val today = LocalDate.of(2026, 10, 25)
        val seed = buildSeed(today)
        val month = YearMonth.from(today)

        assertEquals(5, seed.envelopes.size)
        assertEquals(seed.envelopes.size, seed.envelopes.map { it.id }.toSet().size)
        assertEquals(seed.budgets.size, seed.budgets.map { it.id }.toSet().size)
        assertEquals(seed.expenditures.size, seed.expenditures.map { it.id }.toSet().size)

        assertTrue(seed.budgets.all { budget -> seed.envelopes.any { it.id == budget.envelopeId } })
        assertTrue(seed.allocations.all { allocation ->
            seed.expenditures.any { it.id == allocation.expenditureId } &&
                seed.envelopes.any { it.id == allocation.envelopeId }
        })
        assertTrue(seed.allocations.all { it.amount > 0 })

        seed.budgets.groupBy { it.envelopeId }.values.forEach { budgets ->
            budgets.sortedBy { it.dateSpan.startDate }.zipWithNext().forEach { (left, right) ->
                assertTrue(left.dateSpan.endDate <= right.dateSpan.startDate)
            }
        }

        assertTrue(seed.budgets.all { it.attributedMonth == month })
        assertTrue(seed.budgets.all {
            it.dateSpan.startDate >= today.withDayOfMonth(1) &&
                it.dateSpan.endDate <= today.withDayOfMonth(1).plusMonths(1)
        })
        assertTrue(seed.expenditures.all { it.happenedDate in today.withDayOfMonth(1)..today })
        assertTrue(seed.expenditures.all {
            it.happenedAt == it.happenedDate.atTime(LocalTime.NOON).atZone(zone).toInstant()
        })
    }

    @Test
    fun demoSeedData_coversOverviewScenarios() {
        val today = LocalDate.of(2026, 10, 25)
        val seed = buildSeed(today)
        val activeIds = seed.envelopes.filter { it.archivedAt == null }.map { it.id }.toSet()
        val archived = seed.envelopes.single { it.name == "旧信封" }

        assertNotNull(archived.archivedAt)
        assertTrue(seed.envelopes.filter { it.id in activeIds }.all { it.archivedAt == null })
        assertNull(seed.envelopes.single { it.name == "购物" }.archivedAt)

        val activeAllocated = seed.allocations
            .filter { it.envelopeId in activeIds }
            .sumOf { it.amount }
        val archivedAllocated = seed.allocations
            .filter { it.envelopeId == archived.id }
            .sumOf { it.amount }
        assertEquals(94_000, activeAllocated)
        assertEquals(10_000, archivedAllocated)

        val household = seed.envelopes.single { it.name == "居住" }
        val householdBudgets = seed.budgets.filter { it.envelopeId == household.id }
        assertEquals(5, householdBudgets.size)
        assertEquals(250_000, householdBudgets.sumOf { it.amount })
        assertTrue(householdBudgets.any { today >= it.dateSpan.startDate && today < it.dateSpan.endDate })

        val food = seed.envelopes.single { it.name == "餐饮" }
        assertEquals(300_000, seed.budgets.filter { it.envelopeId == food.id }.sumOf { it.amount })
        assertEquals(33_000, seed.allocations.filter { it.envelopeId == food.id }.sumOf { it.amount })
    }

    @Test
    fun demoSeedData_remainsUsableOnFirstDayOfMonth() {
        val today = LocalDate.of(2026, 10, 1)
        val seed = buildSeed(today)
        val activeIds = seed.envelopes.filter { it.archivedAt == null }.map { it.id }.toSet()

        assertTrue(seed.expenditures.all { it.happenedDate >= today })
        assertEquals(
            68_000,
            seed.allocations.filter { it.envelopeId in activeIds }.sumOf { it.amount },
        )
        assertTrue(seed.budgets.all { it.dateSpan.startDate >= today })
    }
}
