package cn.osmrpighead.envelog.core.number

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode

class NumberTest {
    private val zero: BigDecimal = BigDecimal.ZERO
    private val one: BigDecimal = BigDecimal.ONE

    private fun assertDecimal(expected: String, actual: BigDecimal) {
        assertEquals("expected $expected but was $actual", 0, BigDecimal(expected).compareTo(actual))
    }

    @Test
    fun ratioOf_divisibleValues_isValid() {
        val ratio = 10L.ratioOf(40L)

        assertTrue(ratio.isValid)
        assertDecimal("0.25", ratio.ratio)
    }

    @Test
    fun ratioOf_repeatingDecimal_isRoundedToDecimal32() {
        val ratio = 1L.ratioOf(3L)

        assertTrue(ratio.isValid)
        assertDecimal("0.3333333", ratio.ratio)
    }

    @Test
    fun ratioOf_zeroDenominator_isInvalidAndZero() {
        val ratio = 5L.ratioOf(0L)

        assertFalse(ratio.isValid)
        assertDecimal("0", ratio.ratio)
    }

    @Test
    fun ratioOf_negativeNumerator_keepsSign() {
        val ratio = (-1L).ratioOf(4L)

        assertTrue(ratio.isValid)
        assertDecimal("-0.25", ratio.ratio)
    }

    @Test
    fun clamp_keepsValueInsideStepBands() {
        assertDecimal("0.5", BigDecimal("0.5").clamp(zero, one, BigDecimal("0.01")))
    }

    @Test
    fun clamp_keepsBoundsExact() {
        assertDecimal("0", zero.clamp(zero, one, BigDecimal("0.01")))
        assertDecimal("1", one.clamp(zero, one, BigDecimal("0.01")))
    }

    @Test
    fun clamp_snapsValuesOneStepInsideBounds() {
        assertDecimal("0.01", BigDecimal("0.005").clamp(zero, one, BigDecimal("0.01")))
        assertDecimal("0.99", BigDecimal("0.995").clamp(zero, one, BigDecimal("0.01")))
    }

    @Test
    fun clamp_keepsValuesAtStepBoundary() {
        assertDecimal("0.01", BigDecimal("0.01").clamp(zero, one, BigDecimal("0.01")))
        assertDecimal("0.99", BigDecimal("0.99").clamp(zero, one, BigDecimal("0.01")))
    }

    @Test
    fun clamp_keepsValuesOutsideBounds() {
        assertDecimal("-0.5", BigDecimal("-0.5").clamp(zero, one, BigDecimal("0.01")))
        assertDecimal("1.5", BigDecimal("1.5").clamp(zero, one, BigDecimal("0.01")))
    }

    @Test
    fun asAmount_scalesAndRoundsHalfUp() {
        assertEquals(1235L, BigDecimal("12.345").asAmount(2, RoundingMode.HALF_UP))
        assertEquals(1234L, BigDecimal("12.344").asAmount(2, RoundingMode.HALF_UP))
        assertEquals(1000L, BigDecimal("10").asAmount(2, RoundingMode.HALF_UP))
    }

    @Test
    fun asAmount_scalesAndRoundsDown() {
        assertEquals(99L, BigDecimal("0.999").asAmount(2, RoundingMode.DOWN))
        assertEquals(-99L, BigDecimal("-0.999").asAmount(2, RoundingMode.DOWN))
    }
}
