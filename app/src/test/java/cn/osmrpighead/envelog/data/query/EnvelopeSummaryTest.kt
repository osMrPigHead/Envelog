package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.data.model.EnvelopeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.UUID

class EnvelopeSummaryTest {
    private val envelopeId = EnvelopeId(UUID(0L, 1L))

    private fun budgeted(
        allocated: Long = 30_000,
        totalBudget: Long = 100_000,
        currentBudget: Long? = 40_000,
        currentSpanAllocated: Long = 10_000,
        priorBudget: Long = 50_000,
        priorAllocated: Long = 20_000,
    ): EnvelopeSummary.Budgeted = EnvelopeSummary.Budgeted(
        id = envelopeId,
        name = "餐饮",
        allocated = allocated,
        currency = CurrencyType.CNY,
        totalBudget = totalBudget,
        currentBudget = currentBudget,
        currentSpan = null,
        currentSpanAllocated = currentSpanAllocated,
        priorBudget = priorBudget,
        priorAllocated = priorAllocated,
    )

    @Test
    fun currentAvailable_isCurrentBudgetMinusCurrentSpanAllocated() {
        assertEquals(30_000L, budgeted(currentBudget = 40_000, currentSpanAllocated = 10_000).currentAvailable)
    }

    @Test
    fun currentAvailable_isZeroWithoutCurrentBudget() {
        assertEquals(0L, budgeted(currentBudget = null, currentSpanAllocated = 10_000).currentAvailable)
    }

    @Test
    fun usedProportion_dividesAllocatedByTotalBudget() {
        val proportion = budgeted(allocated = 25_000, totalBudget = 100_000).usedProportion

        assertTrue(proportion.isValid)
        assertEquals(0, BigDecimal("0.25").compareTo(proportion.ratio))
    }

    @Test
    fun usedProportion_isInvalidForZeroTotalBudget() {
        val proportion = budgeted(allocated = 25_000, totalBudget = 0).usedProportion

        assertFalse(proportion.isValid)
        assertEquals(0, BigDecimal.ZERO.compareTo(proportion.ratio))
    }

    @Test
    fun usedProportion_canExceedOne() {
        val proportion = budgeted(allocated = 150_000, totalBudget = 100_000).usedProportion

        assertTrue(proportion.isValid)
        assertEquals(0, BigDecimal("1.5").compareTo(proportion.ratio))
    }

    @Test
    fun priorDifference_isPriorBudgetMinusPriorAllocated() {
        assertEquals(30_000L, budgeted(priorBudget = 50_000, priorAllocated = 20_000).priorDifference)
        assertEquals(-5_000L, budgeted(priorBudget = 5_000, priorAllocated = 10_000).priorDifference)
    }
}
