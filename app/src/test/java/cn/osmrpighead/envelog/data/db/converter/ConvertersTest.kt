package cn.osmrpighead.envelog.data.db.converter

import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class ConvertersTest {
    private val timeConverters = TimeConverters()
    private val idConverters = IdConverters()

    @Test
    fun timeConverters_localDateRoundTrips() {
        val date = LocalDate.of(2026, 10, 20)

        assertEquals("2026-10-20", timeConverters.fromLocalDate(date))
        assertEquals(date, timeConverters.toLocalDate("2026-10-20"))
    }

    @Test
    fun timeConverters_instantRoundTrips() {
        val instant = Instant.parse("2026-10-20T04:00:00Z")

        assertEquals(instant, timeConverters.toInstant(timeConverters.fromInstant(instant)))
    }

    @Test
    fun timeConverters_yearMonthRoundTrips() {
        val month = YearMonth.of(2026, 10)

        assertEquals("2026-10", timeConverters.fromYearMonth(month))
        assertEquals(month, timeConverters.toYearMonth("2026-10"))
    }

    @Test
    fun timeConverters_zoneIdRoundTrips() {
        val zone = ZoneId.of("Asia/Shanghai")

        assertEquals("Asia/Shanghai", timeConverters.fromZoneId(zone))
        assertEquals(zone, timeConverters.toZoneId("Asia/Shanghai"))
    }

    @Test
    fun idConverters_envelopeIdRoundTrips() {
        val id = EnvelopeId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"))

        assertEquals("00000000-0000-0000-0000-0000000000a1", idConverters.fromEnvelopeId(id))
        assertEquals(id, idConverters.toEnvelopeId(idConverters.fromEnvelopeId(id)))
    }

    @Test
    fun idConverters_budgetIdRoundTrips() {
        val id = BudgetId(UUID.fromString("00000000-0000-0000-0000-0000000000d1"))

        assertEquals(id, idConverters.toBudgetId(idConverters.fromBudgetId(id)))
    }

    @Test
    fun idConverters_expenditureIdRoundTrips() {
        val id = ExpenditureId(UUID.fromString("00000000-0000-0000-0000-0000000000c1"))

        assertEquals(id, idConverters.toExpenditureId(idConverters.fromExpenditureId(id)))
    }
}
