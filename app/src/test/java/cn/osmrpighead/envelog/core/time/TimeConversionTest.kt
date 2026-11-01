package cn.osmrpighead.envelog.core.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class TimeConversionTest {
    private val shanghai: ZoneId = ZoneId.of("Asia/Shanghai")

    @Test
    fun instantToLocalDate_dependsOnZone() {
        val instant = Instant.parse("2026-10-31T20:00:00Z")

        assertEquals(LocalDate.of(2026, 11, 1), instant.toLocalDate(shanghai))
        assertEquals(LocalDate.of(2026, 10, 31), instant.toLocalDate(ZoneOffset.UTC))
    }

    @Test
    fun instantToLocalDateTime_keepsTimeInZone() {
        val instant = Instant.parse("2026-10-01T16:30:00Z")

        assertEquals(LocalDateTime.of(2026, 10, 2, 0, 30), instant.toLocalDateTime(shanghai))
    }

    @Test
    fun epochMillisToLocalDate_defaultsToUtc() {
        val millis = Instant.parse("2026-10-31T23:30:00Z").toEpochMilli()

        assertEquals(LocalDate.of(2026, 10, 31), millis.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 1), millis.toLocalDate(shanghai))
    }

    @Test
    fun dateAndInstantRoundTripThroughZone() {
        val date = LocalDate.of(2026, 10, 20)
        val instant = date.atTime(12, 0).atZone(shanghai).toInstant()

        assertEquals(date, instant.toLocalDate(shanghai))
    }
}
