package cn.osmrpighead.envelog.data.repository.room

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
 * 在真实设备的 SQLite 上复验仓库的写入事务：一笔支出分摊到多个信封、归档只生效一次、删除信封的编排与回滚。
 */
@RunWith(AndroidJUnit4::class)
class RoomEnvelogRepositoryInstrumentedTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val happenedDate: LocalDate = LocalDate.of(2026, 10, 5)

    private val deletedAt: Instant = Instant.parse("2026-10-19T04:00:00Z")

    private val envelopeA = EnvelopeId(UUID(0L, 0xA1))
    private val envelopeB = EnvelopeId(UUID(0L, 0xA2))
    private val unknownEnvelope = EnvelopeId(UUID(0L, 0xEE))
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

    private fun budget(value: Long, envelopeId: EnvelopeId, deletedAt: Instant?): BudgetEntity =
        BudgetEntity(
            id = BudgetId(UUID(0L, value)),
            envelopeId = envelopeId,
            dateSpan = month.toHalfOpenDateSpan(),
            attributedMonth = month,
            amount = 100_000,
            deletedAt = deletedAt,
        )

    private fun garbage(
        value: Long,
        envelopeId: EnvelopeId,
        amount: Long,
        softDeletedAt: Instant? = deletedAt,
    ): Pair<ExpenditureEntity, ExpenditureAllocationEntity> {
        val id = ExpenditureId(UUID(0L, value))
        return ExpenditureEntity(
            id = id,
            happenedAt = at(happenedDate),
            happenedDate = happenedDate,
            happenedZone = zone,
            deletedAt = softDeletedAt,
        ) to ExpenditureAllocationEntity(id, envelopeId, amount)
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

    @Test
    fun deleteEnvelope_removesEnvelopeAndOnlyItsSoftDeletedRows() = runBlocking {
        seedEnvelopes()
        database.seedDao().insertBudgets(listOf(budget(0xD1, envelopeA, deletedAt), budget(0xD2, envelopeB, deletedAt)))
        val garbageOfA = garbage(0xC1, envelopeA, 6_000)
        val garbageOfB = garbage(0xC2, envelopeB, 4_000)
        database.seedDao().insertExpenditures(listOf(garbageOfA.first, garbageOfB.first))
        database.seedDao().insertAllocations(listOf(garbageOfA.second, garbageOfB.second))

        assertEquals(EnvelopeDeleteResult.Deleted, repository.deleteEnvelope(envelopeA))

        assertNull(database.envelopeDao().get(envelopeA))
        assertNotNull("其他信封应保持存活", database.envelopeDao().get(envelopeB))
        assertEquals("信封 B 的软删除预算不应被顺带清理", 1, database.budgetDao().clean(envelopeB))
        assertEquals("信封 B 的软删除分摊不应被顺带清理", 1, database.expenditureAllocationDao().clean(envelopeB))
    }

    @Test
    fun deleteEnvelope_reportsNotFoundWithoutTouchingAnyTable() = runBlocking {
        seedEnvelopes()
        val garbageOfA = garbage(0xC1, envelopeA, 6_000)
        database.seedDao().insertExpenditures(listOf(garbageOfA.first))
        database.seedDao().insertAllocations(listOf(garbageOfA.second))

        assertEquals(EnvelopeDeleteResult.NotFound, repository.deleteEnvelope(unknownEnvelope))

        assertNotNull(database.envelopeDao().get(envelopeA))
        assertEquals(1, database.expenditureAllocationDao().clean(envelopeA))
    }

    @Test
    fun deleteEnvelope_reportsConstraintAndRollsBackWhenLiveBudgetRemains() = runBlocking {
        seedEnvelopes()
        database.seedDao().insertBudgets(listOf(budget(0xD1, envelopeA, deletedAt), budget(0xD2, envelopeA, null)))

        assertEquals(EnvelopeDeleteResult.Constraint, repository.deleteEnvelope(envelopeA))

        assertNotNull("真实外键约束应保住信封行", database.envelopeDao().get(envelopeA))
        assertEquals("事务应整体回滚，已清理的软删除预算应仍存在", 1, database.budgetDao().clean(envelopeA))
    }

    @Test
    fun deleteEnvelope_reportsConstraintAndRollsBackWhenLiveAllocationRemains() = runBlocking {
        seedEnvelopes()
        database.seedDao().insertBudgets(listOf(budget(0xD1, envelopeA, deletedAt)))
        val alive = garbage(0xC1, envelopeA, 6_000, softDeletedAt = null)
        database.seedDao().insertExpenditures(listOf(alive.first))
        database.seedDao().insertAllocations(listOf(alive.second))

        assertEquals(EnvelopeDeleteResult.Constraint, repository.deleteEnvelope(envelopeA))

        assertNotNull(database.envelopeDao().get(envelopeA))
        assertEquals("事务应整体回滚，已清理的软删除预算应仍存在", 1, database.budgetDao().clean(envelopeA))
    }
}
