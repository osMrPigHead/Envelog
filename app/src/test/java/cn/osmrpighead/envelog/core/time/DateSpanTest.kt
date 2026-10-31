package cn.osmrpighead.envelog.core.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class DateSpanTest {
    private val start: LocalDate = LocalDate.of(2026, 10, 1)
    private val endExclusive: LocalDate = LocalDate.of(2026, 11, 1)

    @Test
    fun untilExclusive_keepsStartAndExclusiveEnd() {
        val span = start untilExclusive endExclusive

        assertEquals(start, span.startDate)
        assertEquals(endExclusive, span.endDate)
    }

    @Test
    fun untilInclusive_keepsBothBounds() {
        val lastDay = endExclusive.minusDays(1)
        val span = start untilInclusive lastDay

        assertEquals(start, span.startDate)
        assertEquals(lastDay, span.endDate)
    }

    @Test
    fun toClosed_subtractsOneDayFromExclusiveEnd() {
        val closed = (start untilExclusive endExclusive).toClosed()

        assertEquals(start, closed.startDate)
        assertEquals(endExclusive.minusDays(1), closed.endDate)
    }

    @Test
    fun closedToHalfOpen_addsOneDayToInclusiveEnd() {
        val closed = ClosedDateSpan(start, endExclusive.minusDays(1))

        assertEquals(HalfOpenDateSpan(start, endExclusive), closed.toHalfOpen())
    }

    @Test
    fun closedAndHalfOpen_roundTripInBothDirections() {
        val halfOpen = start untilExclusive endExclusive
        val closed = start untilInclusive endExclusive.minusDays(1)

        assertEquals(halfOpen, halfOpen.toClosed().toHalfOpen())
        assertEquals(closed, closed.toHalfOpen().toClosed())
    }

    @Test
    fun countDaysTo_returnsHalfOpenSpanLength() {
        assertEquals(31L, start countDaysTo endExclusive)
        assertEquals(0L, start countDaysTo start)
        assertEquals(-31L, endExclusive countDaysTo start)
    }

    @Test
    fun yearMonth_toHalfOpenDateSpan_coversWholeMonth() {
        val span = YearMonth.of(2026, 10).toHalfOpenDateSpan()

        assertEquals(HalfOpenDateSpan(start, endExclusive), span)
        assertEquals(31L, span.startDate countDaysTo span.endDate)
    }

    @Test
    fun yearMonth_firstAndNext_areAdjacentMonths() {
        val october = YearMonth.of(2026, 10)

        assertEquals(start, october.first)
        assertEquals(YearMonth.of(2026, 11), october.next)
        assertEquals(october.first, october.next.first.minusMonths(1))
    }

    @Test
    fun yearMonth_contains_matchesYearAndMonth() {
        val october = YearMonth.of(2026, 10)

        assertTrue(october.contains(LocalDate.of(2026, 10, 1)))
        assertTrue(october.contains(LocalDate.of(2026, 10, 31)))
        assertFalse(october.contains(endExclusive))
        assertFalse(october.contains(LocalDate.of(2025, 10, 15)))
    }

    @Test
    fun dateOfWeekHelpers_resolveNearestWeekday() {
        val wednesday = LocalDate.of(2026, 10, 21)

        assertEquals(LocalDate.of(2026, 10, 26), wednesday.next(DayOfWeek.MONDAY))
        assertEquals(LocalDate.of(2026, 10, 19), wednesday.previous(DayOfWeek.MONDAY))
        assertEquals(wednesday, wednesday.nextOrSame(DayOfWeek.WEDNESDAY))
        assertEquals(LocalDate.of(2026, 10, 19), wednesday.previousOrSame(DayOfWeek.MONDAY))
    }
}
