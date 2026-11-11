package cn.osmrpighead.envelog.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class DateSpanGeometryTest {
    private val start: LocalDate = LocalDate.of(2026, 10, 1)
    private val endExclusive: LocalDate = LocalDate.of(2026, 11, 1)

    private fun span(start: LocalDate, endExclusive: LocalDate) = HalfOpenDateSpan(start, endExclusive)

    @Test
    fun length_countsDaysBetweenBounds() {
        assertEquals(31L, span(start, endExclusive).length)
        assertEquals(0L, span(start, start).length)
    }

    @Test
    fun length_handlesLeapFebruary() {
        val february = span(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 3, 1))

        assertEquals(29L, february.length)
    }

    @Test
    fun length_isNegativeForReversedSpan() {
        assertEquals(-31L, span(endExclusive, start).length)
    }

    @Test
    fun attributedMonth_usesTheMonthOfASpanInsideOneMonth() {
        assertEquals(YearMonth.of(2026, 10), span(start, endExclusive).attributedMonth)
    }

    @Test
    fun attributedMonth_prefersTheStartMonthWhenItHasTheLargerShare() {
        val crossing = span(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 5))

        assertEquals(YearMonth.of(2026, 9), crossing.attributedMonth)
    }

    @Test
    fun attributedMonth_prefersTheEndMonthWhenItHasTheLargerShare() {
        val crossing = span(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 20))

        assertEquals(YearMonth.of(2026, 11), crossing.attributedMonth)
    }

    @Test
    fun attributedMonth_fallsBackToStartMonthOnEqualShares() {
        val crossing = span(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 10, 9))

        assertEquals(YearMonth.of(2026, 9), crossing.attributedMonth)
    }

    @Test
    fun attributedMonth_fallsBackToStartMonthWhenNoEdgeMonthExceedsHalf() {
        val crossing = span(LocalDate.of(2025, 12, 15), LocalDate.of(2026, 3, 20))

        assertEquals(YearMonth.of(2025, 12), crossing.attributedMonth)
    }

    @Test
    fun attributedMonth_ignoresAnEmptyEndMonth() {
        val crossing = span(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 1))

        assertEquals(YearMonth.of(2026, 9), crossing.attributedMonth)
    }

    @Test
    fun splitIntoWeeks_cutsAtWeekBoundaries() {
        val weeks = span(start, endExclusive).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(
            listOf(
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)),
                span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)),
                span(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19)),
                span(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)),
                span(LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 1)),
            ),
            weeks,
        )
    }

    @Test
    fun splitIntoWeeks_returnsOneSpanWhenShorterThanAWeek() {
        val weeks = span(start, LocalDate.of(2026, 10, 3)).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(listOf(span(start, LocalDate.of(2026, 10, 3))), weeks)
    }

    @Test
    fun splitIntoWeeks_lastWeekEndsAtSpanEnd() {
        val weeks = span(start, LocalDate.of(2026, 10, 8)).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(
            listOf(
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5)),
                span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8)),
            ),
            weeks,
        )
    }

    @Test
    fun splitIntoWeeks_ofEmptySpan_returnsSingleEmptySpan() {
        val weeks = span(start, start).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(listOf(span(start, start)), weeks)
    }

    @Test
    fun splitIntoWeeks_whenStartIsFirstDayOfWeek_doesNotEmitZeroLengthWeek() {
        val weeks = span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 26))
            .splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(
            listOf(
                span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)),
                span(LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 19)),
                span(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 26)),
            ),
            weeks,
        )
    }

    @Test
    fun splitIntoWeeks_weeksAreContiguousAndCoverSpan() {
        val weeks = span(start, endExclusive).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(start, weeks.first().startDate)
        assertEquals(endExclusive, weeks.last().endDate)
        assertEquals(31L, weeks.sumOf { it.length })
        weeks.zipWithNext().forEach { (current, next) ->
            assertEquals(current.endDate, next.startDate)
        }
        weeks.forEach { week -> assertTrue("周长度应为正，实际为 ${week.length}", week.length > 0) }
    }

    @Test
    fun intersect_returnsOverlappingPartAndIsCommutative() {
        val first = span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15))
        val second = span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20))
        val expected = span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 15))

        assertEquals(expected, first intersect second)
        assertEquals(expected, second intersect first)
    }

    @Test
    fun overlaps_requiresStrictOverlap() {
        val first = span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15))
        val second = span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20))
        val before = span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5))
        val after = span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 10))

        assertTrue(first overlaps second)
        assertTrue(second overlaps first)
        assertFalse(before overlaps after)
        assertFalse(after overlaps before)
        assertFalse(span(start, start) overlaps span(start, start))
    }

    @Test
    fun hasBeforeAndAfter_compareWithTheOtherSpan() {
        val base = span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12))

        val same = span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12))
        assertFalse(base hasBefore same)
        assertFalse(base hasAfter same)

        val inner = span(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 10))
        assertTrue(base hasBefore inner)
        assertTrue(base hasAfter inner)

        val startsBefore = span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 12))
        assertFalse(base hasBefore startsBefore)
        assertFalse(base hasAfter startsBefore)

        val endsBefore = span(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 8))
        assertFalse(base hasBefore endsBefore)
        assertTrue(base hasAfter endsBefore)

        val startsAfter = span(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 12))
        assertTrue(base hasBefore startsAfter)
        assertFalse(base hasAfter startsAfter)
    }

    @Test
    fun contains_includesStartAndExcludesEnd() {
        val month = span(start, endExclusive)

        assertTrue(LocalDate.of(2026, 10, 1) in month)
        assertTrue(LocalDate.of(2026, 10, 31) in month)
        assertFalse(LocalDate.of(2026, 11, 1) in month)
        assertFalse(LocalDate.of(2026, 9, 30) in month)
    }

    @Test
    fun iterator_yieldsDatesInOrder() {
        val dates = buildList {
            for (date in span(start, LocalDate.of(2026, 10, 4))) add(date)
        }

        assertEquals(
            listOf(
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2),
                LocalDate.of(2026, 10, 3),
            ),
            dates,
        )
    }

    @Test
    fun iterator_ofEmptySpan_isExhausted() {
        val iterator = span(start, start).iterator()

        assertFalse(iterator.hasNext())
        assertThrows(NoSuchElementException::class.java) { iterator.next() }
    }

    @Test
    fun hasOverlap_detectsOverlapAndAllowsAdjacentOrUnsortedSpans() {
        assertTrue(
            listOf(
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)),
                span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20)),
            ).hasOverlap(),
        )
        assertTrue(
            listOf(
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)),
                span(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)),
            ).hasOverlap(),
        )
        assertFalse(
            listOf(
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)),
                span(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20)),
            ).hasOverlap(),
        )
        assertFalse(
            listOf(
                span(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20)),
                span(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15)),
            ).hasOverlap(),
        )
    }
}
