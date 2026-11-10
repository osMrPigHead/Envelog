package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExpenditureAllocationDaoTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var dao: ExpenditureAllocationDao

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        dao = database.expenditureAllocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun allocations(
        spanStart: LocalDate = RoomTestData.monthStart,
        spanEnd: LocalDate = RoomTestData.monthEnd,
    ): List<AllocationWithInfoRow> =
        dao.observe(RoomTestData.envelopeA, spanStart, spanEnd).first()

    private suspend fun daily(
        spanStart: LocalDate = RoomTestData.monthStart,
        spanEnd: LocalDate = RoomTestData.monthEnd,
    ): List<DailyAllocationSummary> =
        dao.observeDailySummaries(RoomTestData.envelopeA, spanStart, spanEnd).first()

    private suspend fun seedMixedRecords() {
        val (firstDay, firstDayShares) = RoomTestData.record(
            0xB1,
            LocalDate.of(2026, 10, 1),
            RoomTestData.envelopeA to 2_000,
        )
        val (sameDay, sameDayShares) = RoomTestData.record(
            0xB2,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 6_000,
            RoomTestData.envelopeB to 4_000,
        )
        val (anotherSameDay, anotherSameDayShares) = RoomTestData.record(
            0xB3,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 1_000,
        )
        val (lastDay, lastDayShares) = RoomTestData.record(
            0xB4,
            LocalDate.of(2026, 10, 31),
            RoomTestData.envelopeA to 5_000,
        )
        val (nextMonth, nextMonthShares) = RoomTestData.record(
            0xB5,
            LocalDate.of(2026, 11, 1),
            RoomTestData.envelopeA to 7_000,
        )
        val (previousMonth, previousMonthShares) = RoomTestData.record(
            0xB6,
            LocalDate.of(2026, 9, 30),
            RoomTestData.envelopeA to 3_000,
        )
        val (deleted, deletedShares) = RoomTestData.record(
            0xB7,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 9_000,
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            expenditures = listOf(
                firstDay,
                sameDay,
                anotherSameDay,
                lastDay,
                nextMonth,
                previousMonth,
                deleted.copy(deletedAt = Instant.parse("2026-10-19T04:00:00Z")),
            ),
            allocations = firstDayShares + sameDayShares + anotherSameDayShares +
                lastDayShares + nextMonthShares + previousMonthShares + deletedShares,
        )
    }

    @Test
    fun observeAllocations_returnsEnvelopeRowsWithinHalfOpenSpan() = runTest {
        seedMixedRecords()

        val rows = allocations()

        assertEquals(4, rows.size)
        assertEquals(
            setOf(
                AllocationWithInfoRow(
                    expenditureId = RoomTestData.expenditureId(0xB1),
                    happenedAt = RoomTestData.at(LocalDate.of(2026, 10, 1)),
                    happenedDate = LocalDate.of(2026, 10, 1),
                    happenedZone = RoomTestData.zone,
                    envelopeId = RoomTestData.envelopeA,
                    amount = 2_000,
                ),
                AllocationWithInfoRow(
                    expenditureId = RoomTestData.expenditureId(0xB2),
                    happenedAt = RoomTestData.at(LocalDate.of(2026, 10, 5)),
                    happenedDate = LocalDate.of(2026, 10, 5),
                    happenedZone = RoomTestData.zone,
                    envelopeId = RoomTestData.envelopeA,
                    amount = 6_000,
                ),
                AllocationWithInfoRow(
                    expenditureId = RoomTestData.expenditureId(0xB3),
                    happenedAt = RoomTestData.at(LocalDate.of(2026, 10, 5)),
                    happenedDate = LocalDate.of(2026, 10, 5),
                    happenedZone = RoomTestData.zone,
                    envelopeId = RoomTestData.envelopeA,
                    amount = 1_000,
                ),
                AllocationWithInfoRow(
                    expenditureId = RoomTestData.expenditureId(0xB4),
                    happenedAt = RoomTestData.at(LocalDate.of(2026, 10, 31)),
                    happenedDate = LocalDate.of(2026, 10, 31),
                    happenedZone = RoomTestData.zone,
                    envelopeId = RoomTestData.envelopeA,
                    amount = 5_000,
                ),
            ),
            rows.toSet(),
        )
    }

    @Test
    fun observeAllocations_includesSpanStartAndExcludesSpanEnd() = runTest {
        seedMixedRecords()

        val rows = allocations()
        assertTrue(rows.any { it.happenedDate == LocalDate.of(2026, 10, 1) })
        assertTrue(rows.any { it.happenedDate == LocalDate.of(2026, 10, 31) })
        assertTrue(rows.none { it.happenedDate == LocalDate.of(2026, 11, 1) })
    }

    @Test
    fun observeDailyAllocationSummaries_groupsByDateAndEnvelope() = runTest {
        seedMixedRecords()

        val rows = daily()

        assertEquals(3, rows.size)
        assertEquals(
            mapOf(
                LocalDate.of(2026, 10, 1) to 2_000L,
                LocalDate.of(2026, 10, 5) to 7_000L,
                LocalDate.of(2026, 10, 31) to 5_000L,
            ),
            rows.associate { it.happenedDate to it.amount },
        )
        assertEquals(
            setOf(RoomTestData.envelopeA),
            rows.map { it.envelopeId }.toSet(),
        )
    }

    @Test
    fun observeDailyAllocationSummaries_addsUpSharesOfTheSameDate() = runTest {
        seedMixedRecords()

        assertEquals(7_000L, daily().single { it.happenedDate == LocalDate.of(2026, 10, 5) }.amount)
    }

    @Test
    fun observeAllocations_excludesSoftDeletedExpenditures() = runTest {
        seedMixedRecords()

        assertEquals(
            4,
            allocations().size,
        )
        assertTrue(
            allocations().none { it.expenditureId == RoomTestData.expenditureId(0xB7) },
        )
    }

    @Test
    fun observeDailyAllocationSummaries_excludesSoftDeletedExpenditures() = runTest {
        seedMixedRecords()

        assertEquals(
            7_000L,
            daily().single { it.happenedDate == LocalDate.of(2026, 10, 5) }.amount,
        )
    }
}
