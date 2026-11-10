package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * 在真实设备的 SQLite 上复验分摊明细查询与按日汇总查询。
 *
 * 与 test 源根的 Robolectric 版本用例对应，用于确认真实的 SQLite 排序与聚合行为一致。
 */
@RunWith(AndroidJUnit4::class)
class AllocationQueryInstrumentedTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)

    private val envelopeA = EnvelopeId(UUID(0L, 0xA1))
    private val envelopeB = EnvelopeId(UUID(0L, 0xA2))

    private lateinit var database: EnvelogDatabase
    private lateinit var dao: ExpenditureAllocationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnvelogDatabase::class.java,
        ).build()
        dao = database.expenditureAllocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun at(date: LocalDate): Instant =
        date.atTime(LocalTime.NOON).atZone(zone).toInstant()

    private fun envelope(id: EnvelopeId, name: String, isBudgeted: Boolean = true): EnvelopeEntity =
        EnvelopeEntity(id, name, isBudgeted, archivedAt = null)

    private fun record(
        value: Long,
        date: LocalDate,
        vararg shares: Pair<EnvelopeId, Long>,
        deletedAt: Instant? = null,
    ): Pair<ExpenditureEntity, List<ExpenditureAllocationEntity>> {
        val id = ExpenditureId(UUID(0L, value))
        val expenditure = ExpenditureEntity(
            id = id,
            happenedAt = at(date),
            happenedDate = date,
            happenedZone = zone,
            deletedAt = deletedAt,
        )
        return expenditure to shares.map { (envelopeId, amount) ->
            ExpenditureAllocationEntity(id, envelopeId, amount)
        }
    }

    private suspend fun seed(records: List<Pair<ExpenditureEntity, List<ExpenditureAllocationEntity>>>) {
        val seedDao = database.seedDao()
        seedDao.insertEnvelopes(
            listOf(
                envelope(envelopeA, "餐饮"),
                envelope(envelopeB, "购物", isBudgeted = false),
            ),
        )
        seedDao.insertExpenditures(records.map { it.first })
        seedDao.insertAllocations(records.flatMap { it.second })
    }

    private suspend fun allocations(): List<AllocationWithInfoRow> =
        dao.observe(envelopeA, monthStart, monthEnd).first()

    private suspend fun daily(): List<DailyAllocationSummary> =
        dao.observeDailySummaries(envelopeA, monthStart, monthEnd).first()

    @Test
    fun allocations_filterEnvelopeAndSpanAndCarryExpenditureInfo() = runBlocking {
        seed(
            listOf(
                record(0xB1, LocalDate.of(2026, 10, 1), envelopeA to 2_000),
                record(0xB2, LocalDate.of(2026, 10, 5), envelopeA to 6_000, envelopeB to 4_000),
                record(0xB3, LocalDate.of(2026, 10, 31), envelopeA to 5_000),
                record(0xB4, LocalDate.of(2026, 11, 1), envelopeA to 7_000),
                record(0xB5, LocalDate.of(2026, 9, 30), envelopeA to 3_000),
            ),
        )

        val rows = allocations()

        assertEquals(3, rows.size)
        assertEquals(
            setOf(
                AllocationWithInfoRow(
                    ExpenditureId(UUID(0L, 0xB1)),
                    at(LocalDate.of(2026, 10, 1)),
                    LocalDate.of(2026, 10, 1),
                    zone,
                    envelopeA,
                    2_000,
                ),
                AllocationWithInfoRow(
                    ExpenditureId(UUID(0L, 0xB2)),
                    at(LocalDate.of(2026, 10, 5)),
                    LocalDate.of(2026, 10, 5),
                    zone,
                    envelopeA,
                    6_000,
                ),
                AllocationWithInfoRow(
                    ExpenditureId(UUID(0L, 0xB3)),
                    at(LocalDate.of(2026, 10, 31)),
                    LocalDate.of(2026, 10, 31),
                    zone,
                    envelopeA,
                    5_000,
                ),
            ),
            rows.toSet(),
        )
    }

    @Test
    fun dailySummaries_groupByDateAndEnvelope() = runBlocking {
        seed(
            listOf(
                record(0xB1, LocalDate.of(2026, 10, 1), envelopeA to 2_000),
                record(0xB2, LocalDate.of(2026, 10, 5), envelopeA to 6_000, envelopeB to 4_000),
                record(0xB3, LocalDate.of(2026, 10, 5), envelopeA to 1_000),
                record(0xB4, LocalDate.of(2026, 10, 31), envelopeA to 5_000),
                record(0xB5, LocalDate.of(2026, 11, 1), envelopeA to 7_000),
            ),
        )

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
        assertEquals(setOf(envelopeA), rows.map { it.envelopeId }.toSet())
    }

    @Test
    fun allocationsAndDailySummaries_excludeSoftDeletedExpenditures() = runBlocking {
        val deletedAt = Instant.parse("2026-10-19T04:00:00Z")
        seed(
            listOf(
                record(0xB1, LocalDate.of(2026, 10, 5), envelopeA to 6_000),
                record(0xB2, LocalDate.of(2026, 10, 5), envelopeA to 9_000, deletedAt = deletedAt),
                record(0xB3, LocalDate.of(2026, 10, 6), envelopeA to 2_000, deletedAt = deletedAt),
            ),
        )

        assertEquals(1, allocations().size)
        assertEquals(6_000L, allocations().single().amount)
        assertEquals(1, daily().size)
        assertEquals(6_000L, daily().single().amount)
    }
}
