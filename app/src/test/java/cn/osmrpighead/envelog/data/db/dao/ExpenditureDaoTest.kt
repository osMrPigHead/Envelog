package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ExpenditureDaoTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var dao: ExpenditureDao
    private lateinit var allocationDao: ExpenditureAllocationDao

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        dao = database.expenditureDao()
        allocationDao = database.expenditureAllocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val deletedAt: Instant = Instant.parse("2026-10-19T04:00:00Z")

    /**
     * B1 软删除且只分摊到 A、B2 软删除且同时分摊到 A 与 B、B3 存活、B4 软删除且只分摊到 B、B5 软删除且无分摊。
     */
    private suspend fun seedMixedRecords() {
        val (onlyA, onlyAShares) = RoomTestData.record(
            0xB1,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 1_000,
        )
        val (bothEnvelopes, bothShares) = RoomTestData.record(
            0xB2,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 1_000,
            RoomTestData.envelopeB to 2_000,
        )
        val (alive, aliveShares) = RoomTestData.record(
            0xB3,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 1_000,
            RoomTestData.envelopeB to 2_000,
        )
        val (onlyB, onlyBShares) = RoomTestData.record(
            0xB4,
            LocalDate.of(2026, 10, 6),
            RoomTestData.envelopeB to 3_000,
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            expenditures = listOf(
                onlyA.copy(deletedAt = deletedAt),
                bothEnvelopes.copy(deletedAt = deletedAt),
                alive,
                onlyB.copy(deletedAt = deletedAt),
                RoomTestData.expenditure(RoomTestData.expenditureId(0xB5), deletedAt = deletedAt),
            ),
            allocations = onlyAShares + bothShares + aliveShares + onlyBShares,
        )
    }

    @Test
    fun clean_deletesSoftDeletedExpenditureWhoseAllocationsAllBelongToTheEnvelope() = runTest {
        seedMixedRecords()

        assertEquals(1, dao.clean(RoomTestData.envelopeA))
        assertEquals("已删除的支出不应再次命中", 0, dao.clean(RoomTestData.envelopeA))
    }

    @Test
    fun clean_keepsSoftDeletedExpenditureSharedWithOtherEnvelopes() = runTest {
        seedMixedRecords()

        dao.clean(RoomTestData.envelopeA)
        assertEquals("只分摊到 B 的软删除支出由 B 的清理负责", 1, dao.clean(RoomTestData.envelopeB))
        assertEquals(0, dao.clean(RoomTestData.envelopeB))
    }

    @Test
    fun clean_cascadesToTheDeletedExpendituresAllocations() = runTest {
        seedMixedRecords()

        assertEquals(1, dao.clean(RoomTestData.envelopeA))
        assertEquals(
            "B1 的分摊应随支出级联删除，只剩 B2 在 A 上的分摊",
            1,
            allocationDao.clean(RoomTestData.envelopeA),
        )
    }

    @Test
    fun clean_findsNoCandidateAfterAllocationsWereCleanedFirst() = runTest {
        seedMixedRecords()

        assertEquals(2, allocationDao.clean(RoomTestData.envelopeA))
        assertEquals("分摊行先被删除后无法再判定孤儿支出", 0, dao.clean(RoomTestData.envelopeA))
    }

    @Test
    fun clean_keepsLiveExpendituresAndTheirAllocations() = runTest {
        seedMixedRecords()

        dao.clean(RoomTestData.envelopeA)
        dao.clean(RoomTestData.envelopeB)

        val rows = allocationDao.observe(
            RoomTestData.envelopeA,
            RoomTestData.monthStart,
            RoomTestData.monthEnd,
        ).first()
        assertEquals(listOf(1_000L), rows.map { it.amount })
    }

    @Test
    fun clean_returnsZeroWhenTheEnvelopeHasNoExpenditure() = runTest {
        seedMixedRecords()

        assertEquals(0, dao.clean(RoomTestData.envelopeId(0xEE)))
    }
}
