package cn.osmrpighead.envelog.data.repository.room

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * 在真实设备的 SQLite 上复验仓库的写入事务：一笔支出分摊到多个信封、归档只生效一次。
 */
@RunWith(AndroidJUnit4::class)
class RoomEnvelogRepositoryInstrumentedTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val happenedDate: LocalDate = LocalDate.of(2026, 10, 5)

    private val envelopeA = EnvelopeId(UUID(0L, 0xA1))
    private val envelopeB = EnvelopeId(UUID(0L, 0xA2))
    private val expenditureId = ExpenditureId(UUID(0L, 0xC1))

    private lateinit var database: EnvelogDatabase
    private lateinit var repository: RoomEnvelogRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnvelogDatabase::class.java,
        ).build()
        repository = RoomEnvelogRepository(
            db = database,
            clock = Clock.fixed(at(happenedDate), zone),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun at(date: LocalDate): Instant =
        date.atTime(LocalTime.NOON).atZone(zone).toInstant()

    private suspend fun seedEnvelopes() {
        database.seedDao().insertEnvelopes(
            listOf(
                EnvelopeEntity(envelopeA, "餐饮", isBudgeted = true, archivedAt = null),
                EnvelopeEntity(envelopeB, "购物", isBudgeted = false, archivedAt = null),
            ),
        )
    }

    @Test
    fun recordExpenditure_persistsAllocationsForEveryEnvelope() = runBlocking {
        seedEnvelopes()
        val submission = ExpenditureSubmission(
            id = expenditureId,
            happenedAt = at(happenedDate),
            happenedZone = zone,
            allocations = listOf(
                ExpenditureAllocation(expenditureId, envelopeA, 6_000),
                ExpenditureAllocation(expenditureId, envelopeB, 4_000),
            ),
        )

        repository.recordExpenditure(submission)

        val span = month.toHalfOpenDateSpan()
        val ofA = repository.observeAllocations(envelopeA, span).first()
        val ofB = repository.observeAllocations(envelopeB, span).first()
        assertEquals(1, ofA.size)
        assertEquals(6_000L, ofA.single().amount)
        assertEquals(happenedDate, ofA.single().expenditure.happenedDate)
        assertEquals(1, ofB.size)
        assertEquals(4_000L, ofB.single().amount)
    }

    @Test
    fun archiveEnvelope_marksOnceThenReportsNotFound() = runBlocking {
        seedEnvelopes()

        assertEquals(ArchiveResult.Archived, repository.archiveEnvelope(envelopeA))
        assertNotNull(database.envelopeDao().get(envelopeA)?.archivedAt)
        assertEquals(ArchiveResult.NotFoundOrAlreadyArchived, repository.archiveEnvelope(envelopeA))
    }
}
