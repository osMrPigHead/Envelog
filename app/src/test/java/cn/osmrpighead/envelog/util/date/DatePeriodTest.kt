package cn.osmrpighead.envelog.util.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class DatePeriodTest {
    private val start: LocalDate = LocalDate.of(2026, 10, 1)
    private val end: LocalDate = LocalDate.of(2026, 11, 1)

    @Test
    fun untilExclusive_createsHalfOpenPeriod() {
        val period = start untilExclusive end

        assertEquals(start, period.start)
        assertEquals(end, period.endExclusive)
    }

    @Test
    fun length_isDaysBetweenStartAndExclusiveEnd() {
        assertEquals(31L, (start untilExclusive end).length)
    }

    @Test
    fun contains_excludesEndExclusive() {
        val period = start untilExclusive end

        assertTrue(start in period)
        assertTrue(end.minusDays(1) in period)
        assertFalse(end in period)
        assertFalse(start.minusDays(1) in period)
    }

    @Test
    fun iterator_visitsEveryDayInHalfOpenRange() {
        val days = buildList { for (date in start untilExclusive end) add(date) }

        assertEquals(31, days.size)
        assertEquals(start, days.first())
        assertEquals(end.minusDays(1), days.last())
    }

    @Test
    fun adjacentPeriods_doNotOverlap() {
        val first = start untilExclusive start.plusDays(10)
        val second = start.plusDays(10) untilExclusive start.plusDays(20)

        assertTrue(first fullyBefore second)
        assertTrue(second fullyAfter first)
        assertFalse(first overlap second)
        assertFalse(second overlap first)
    }

    @Test
    fun intersect_returnsCommonRange() {
        val first = start untilExclusive start.plusDays(10)
        val second = start.plusDays(5) untilExclusive start.plusDays(15)

        assertEquals(start.plusDays(5) untilExclusive start.plusDays(10), first intersect second)
    }

    @Test
    fun splitIntoWeeks_coversPeriodWithoutGaps() {
        val weeks = (start untilExclusive end).splitIntoWeeks(DayOfWeek.MONDAY)

        assertEquals(start, weeks.first().start)
        assertEquals(end, weeks.last().endExclusive)
        assertEquals(31L, weeks.sumOf { it.length })
        weeks.zipWithNext { a, b -> assertEquals(a.endExclusive, b.start) }
    }

    @Test
    fun toClosed_subtractsOneDayFromExclusiveEnd() {
        val closed = (start untilExclusive end).toClosed()

        assertEquals(start, closed.start)
        assertEquals(end.minusDays(1), closed.endInclusive)
    }

    @Test
    fun toClosedThenToHalfOpen_roundTrips() {
        val period = start untilExclusive end

        assertEquals(period, period.toClosed().toHalfOpen())
    }
}
