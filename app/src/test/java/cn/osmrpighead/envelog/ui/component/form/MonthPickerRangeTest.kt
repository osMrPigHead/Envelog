package cn.osmrpighead.envelog.ui.component.form

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class MonthPickerRangeTest {
    private val june2026 = YearMonth.of(2026, 6)
    private val september2026 = YearMonth.of(2026, 9)

    @Test
    fun unbounded_containsEveryMonth() {
        val range = MonthPickerRange()

        assertTrue(range.contains(YearMonth.of(1900, 1)))
        assertTrue(range.contains(YearMonth.of(2026, 6)))
        assertTrue(range.contains(YearMonth.of(2100, 12)))
    }

    @Test
    fun boundsAreInclusive() {
        val range = MonthPickerRange(june2026, september2026)

        assertFalse(range.contains(YearMonth.of(2026, 5)))
        assertTrue(range.contains(june2026))
        assertTrue(range.contains(YearMonth.of(2026, 7)))
        assertTrue(range.contains(september2026))
        assertFalse(range.contains(YearMonth.of(2026, 10)))
    }

    @Test
    fun reversedBounds_throw() {
        assertThrows(IllegalArgumentException::class.java) {
            MonthPickerRange(september2026, june2026)
        }
    }

    @Test
    fun coerce_clampsToBounds() {
        val range = MonthPickerRange(june2026, september2026)

        assertEquals(june2026, range.coerce(YearMonth.of(2026, 1)))
        assertEquals(YearMonth.of(2026, 7), range.coerce(YearMonth.of(2026, 7)))
        assertEquals(september2026, range.coerce(YearMonth.of(2026, 12)))
    }

    @Test
    fun unbounded_effectiveYearRangeUsesRadius() {
        val range = MonthPickerRange()

        assertEquals(2024..2030, range.effectiveYearRange(YearMonth.of(2027, 5), radius = 3))
    }

    @Test
    fun lowerBound_effectiveYearRangeStartsAtLowerBound() {
        val range = MonthPickerRange(minYearMonth = june2026)

        assertEquals(2026..2029, range.effectiveYearRange(YearMonth.of(2026, 6), radius = 3))
        assertEquals(2026..2032, range.effectiveYearRange(YearMonth.of(2029, 1), radius = 3))
    }

    @Test
    fun upperBound_effectiveYearRangeEndsAtUpperBound() {
        val range = MonthPickerRange(maxYearMonth = september2026)

        assertEquals(2023..2026, range.effectiveYearRange(YearMonth.of(2026, 9), radius = 3))
        assertEquals(2017..2026, range.effectiveYearRange(YearMonth.of(2020, 1), radius = 3))
    }

    @Test
    fun bothBounds_effectiveYearRangeUsesExactYearSpan() {
        val range = MonthPickerRange(june2026, YearMonth.of(2027, 2))

        assertEquals(2026..2027, range.effectiveYearRange(YearMonth.of(2026, 8), radius = 3))
    }

    @Test
    fun effectiveYearRange_coercesReferenceBeforeLowerBound() {
        val range = MonthPickerRange(minYearMonth = june2026)

        assertEquals(2026..2029, range.effectiveYearRange(YearMonth.of(2020, 1), radius = 3))
    }

    @Test
    fun effectiveYearRange_coercesReferenceAfterUpperBound() {
        val range = MonthPickerRange(maxYearMonth = september2026)

        assertEquals(2023..2026, range.effectiveYearRange(YearMonth.of(2030, 1), radius = 3))
    }

    @Test
    fun monthsIn_limitsBoundaryYears() {
        val range = MonthPickerRange(june2026, YearMonth.of(2027, 2))

        assertEquals((6..12).toList(), range.monthsIn(2026))
        assertEquals(listOf(1, 2), range.monthsIn(2027))
    }

    @Test
    fun monthsIn_singleMonthRangeReturnsOnlyThatMonth() {
        val range = MonthPickerRange(september2026, september2026)

        assertEquals(listOf(9), range.monthsIn(2026))
    }
}
