package cn.osmrpighead.envelog.data.db.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * 在真实设备的 SQLite 上复验信封删除路径：外键 RESTRICT 的拒绝时机、软删除清理的边界与级联，
 * 以及 `isDeletable` 的判断是否与 RESTRICT 的实际拒绝一致。
 */
@RunWith(AndroidJUnit4::class)
class EnvelopeDeleteInstrumentedTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)
    private val today: LocalDate = LocalDate.of(2026, 10, 20)

    private val deletedAt: Instant = Instant.parse("2026-10-19T04:00:00Z")

    private val envelopeA = EnvelopeId(UUID(0L, 0xA1))
    private val envelopeB = EnvelopeId(UUID(0L, 0xA2))
    private val unknownEnvelope = EnvelopeId(UUID(0L, 0xEE))
    private val unknownBudget = BudgetId(UUID(0L, 0xEF))

    private lateinit var database: EnvelogDatabase
    private lateinit var seedDao: SeedDao
    private lateinit var envelopeDao: EnvelopeDao
    private lateinit var budgetDao: BudgetDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var allocationDao: ExpenseAllocationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnvelogDatabase::class.java,
        ).build()
        seedDao = database.seedDao()
        envelopeDao = database.envelopeDao()
        budgetDao = database.budgetDao()
        expenseDao = database.expenseDao()
        allocationDao = database.expenseAllocationDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun at(date: LocalDate): Instant =
        date.atTime(LocalTime.NOON).atZone(zone).toInstant()

    private fun envelope(id: EnvelopeId, name: String, isBudgeted: Boolean = true): EnvelopeEntity =
        EnvelopeEntity(id, name, isBudgeted, archivedAt = null)

    private fun budget(
        value: Long,
        envelopeId: EnvelopeId = envelopeA,
        deletedAt: Instant? = null,
    ): BudgetEntity = BudgetEntity(
        id = BudgetId(UUID(0L, value)),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(monthStart, monthEnd),
        attributedMonth = month,
        amount = 100_000,
        deletedAt = deletedAt,
    )

    private fun record(
        value: Long,
        date: LocalDate,
        vararg shares: Pair<EnvelopeId, Long>,
        deletedAt: Instant? = null,
    ): Pair<ExpenseEntity, List<ExpenseAllocationEntity>> {
        val id = ExpenseId(UUID(0L, value))
        val expense = ExpenseEntity(
            id = id,
            happenedAt = at(date),
            happenedDate = date,
            happenedZone = zone,
            deletedAt = deletedAt,
        )
        return expense to shares.map { (envelopeId, amount) ->
            ExpenseAllocationEntity(id, envelopeId, amount)
        }
    }

    private suspend fun seedEnvelopes() {
        seedDao.insertEnvelopes(
            listOf(
                envelope(envelopeA, "餐饮"),
                envelope(envelopeB, "购物", isBudgeted = false),
            ),
        )
    }

    /**
     * B1 软删除且只分摊到 A、B2 软删除且同时分摊到 A 与 B、B3 存活、B4 软删除且只分摊到 B、B5 软删除且无分摊。
     */
    private suspend fun seedMixedRecords() {
        seedEnvelopes()
        val (onlyA, onlyAShares) = record(0xB1, LocalDate.of(2026, 10, 5), envelopeA to 1_000, deletedAt = deletedAt)
        val (bothEnvelopes, bothShares) = record(
            0xB2,
            LocalDate.of(2026, 10, 5),
            envelopeA to 1_000,
            envelopeB to 2_000,
            deletedAt = deletedAt,
        )
        val (alive, aliveShares) = record(0xB3, LocalDate.of(2026, 10, 5), envelopeA to 1_000, envelopeB to 2_000)
        val (onlyB, onlyBShares) = record(0xB4, LocalDate.of(2026, 10, 6), envelopeB to 3_000, deletedAt = deletedAt)

        seedDao.insertExpenses(
            listOf(onlyA, bothEnvelopes, alive, onlyB, record(0xB5, LocalDate.of(2026, 10, 7), deletedAt = deletedAt).first),
        )
        seedDao.insertAllocations(onlyAShares + bothShares + aliveShares + onlyBShares)
    }

    @Test
    fun delete_isRejectedWhileALiveBudgetReferencesTheEnvelope() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1)))

        val error = runCatching { envelopeDao.delete(envelopeA) }.exceptionOrNull()
        assertTrue("存活的预算应以 FOREIGN KEY 约束拒绝删除，实际为 $error", error is SQLiteConstraintException)

        assertNotNull("被拒绝的删除不应动到信封行", envelopeDao.get(envelopeA))
        assertEquals(1L, budgetDao.countOverlaps(envelopeA, unknownBudget, monthStart, monthEnd))
    }

    @Test
    fun delete_isRejectedWhileALiveAllocationReferencesTheEnvelope() = runBlocking {
        seedMixedRecords()

        val error = runCatching { envelopeDao.delete(envelopeA) }.exceptionOrNull()
        assertTrue("存活支出的分摊行应以 FOREIGN KEY 约束拒绝删除，实际为 $error", error is SQLiteConstraintException)

        assertNotNull(envelopeDao.get(envelopeA))
        assertEquals("拒绝删除后软删除支出的分摊行应全部保留", 2, allocationDao.clean(envelopeA))
    }

    @Test
    fun delete_removesTheEnvelopeOnceItsSoftDeletedRowsAreCleaned() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1, deletedAt = deletedAt), budget(0xD2, envelopeB, deletedAt)))
        val (garbageOfA, sharesOfA) = record(0xB1, LocalDate.of(2026, 10, 5), envelopeA to 6_000, deletedAt = deletedAt)
        val (garbageOfB, sharesOfB) = record(0xB2, LocalDate.of(2026, 10, 6), envelopeB to 3_000, deletedAt = deletedAt)
        seedDao.insertExpenses(listOf(garbageOfA, garbageOfB))
        seedDao.insertAllocations(sharesOfA + sharesOfB)

        assertEquals(1, expenseDao.clean(envelopeA))
        assertEquals("支出行被删除时分摊应随 CASCADE 一并消失", 0, allocationDao.clean(envelopeA))
        assertEquals(1, budgetDao.clean(envelopeA))
        assertEquals(1, envelopeDao.delete(envelopeA))

        assertNull(envelopeDao.get(envelopeA))
        assertNotNull("其他信封应保持存活", envelopeDao.get(envelopeB))
        assertEquals("信封 B 的软删除预算不应被顺带清理", 1, budgetDao.clean(envelopeB))
        assertEquals("信封 B 的软删除分摊不应被顺带清理", 1, allocationDao.clean(envelopeB))
    }

    @Test
    fun delete_returnsZeroForUnknownEnvelope() = runBlocking {
        seedEnvelopes()

        assertEquals(0, envelopeDao.delete(unknownEnvelope))
    }

    @Test
    fun budgetClean_releasesThePrimaryKeyForReinsert() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1)))
        budgetDao.delete(BudgetId(UUID(0L, 0xD1)), deletedAt)

        val conflict = runCatching { budgetDao.insert(budget(0xD1)) }.exceptionOrNull()
        assertTrue("清理前同一主键属于软删除行，插入应失败", conflict is SQLiteConstraintException)

        assertEquals(1, budgetDao.clean(envelopeA))
        budgetDao.insert(budget(0xD1))

        assertEquals("清理后主键应可再次使用", 1L, budgetDao.countOverlaps(envelopeA, unknownBudget, monthStart, monthEnd))
    }

    @Test
    fun expenseClean_deletesOnlyOrphansOfTheEnvelopeAndCascadesAllocations() = runBlocking {
        seedMixedRecords()

        assertEquals(1, expenseDao.clean(envelopeA))
        assertEquals("B1 的分摊应随支出级联删除，只剩 B2 在 A 上的分摊", 1, allocationDao.clean(envelopeA))
    }

    @Test
    fun expenseClean_keepsRecordsSharedWithOtherEnvelopes() = runBlocking {
        seedMixedRecords()

        expenseDao.clean(envelopeA)
        assertEquals("同时分摊到 A 与 B 的软删除支出应由 B 的清理负责", 1, expenseDao.clean(envelopeB))
        assertEquals(0, expenseDao.clean(envelopeB))
    }

    @Test
    fun expenseClean_findsNoCandidateOnceAllocationsWereCleanedFirst() = runBlocking {
        seedMixedRecords()

        assertEquals(2, allocationDao.clean(envelopeA))
        assertEquals("分摊行先被删除后真实 SQLite 也无法再判定孤儿支出", 0, expenseDao.clean(envelopeA))
    }

    /**
     * 汇总查询里的 `canDelete` 列按信封给出，用于复验它与逐 id 的 `isDeletable` 判断口径一致。
     */
    private suspend fun canDeleteByEnvelope(): Map<EnvelopeId, Boolean> = envelopeDao
        .observeSummaries(today = today, yearMonth = month, spanStart = monthStart, spanEnd = monthEnd)
        .first()
        .associate { it.id to it.canDelete }

    @Test
    fun isDeletable_isTrueWhileNothingReferencesTheEnvelope() = runBlocking {
        seedEnvelopes()

        assertTrue(envelopeDao.isDeletable(envelopeA))
        assertTrue(envelopeDao.isDeletable(envelopeB))
        assertTrue("未知信封没有引用行，判断同样放行", envelopeDao.isDeletable(unknownEnvelope))
        assertEquals(
            "汇总查询的可删除列应与逐 id 判断一致",
            mapOf(envelopeA to true, envelopeB to true),
            canDeleteByEnvelope(),
        )
    }

    @Test
    fun isDeletable_agreesWithTheForeignKeyWhenALiveBudgetRemains() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1)))

        assertFalse("存活预算会让 DELETE 命中 RESTRICT", envelopeDao.isDeletable(envelopeA))
        assertTrue("A 的预算不挡住 B", envelopeDao.isDeletable(envelopeB))
        assertEquals(mapOf(envelopeA to false, envelopeB to true), canDeleteByEnvelope())
    }

    @Test
    fun isDeletable_agreesWithTheForeignKeyWhenALiveAllocationRemains() = runBlocking {
        seedMixedRecords()

        assertFalse("B3 存活且分摊到 A", envelopeDao.isDeletable(envelopeA))
        assertFalse("B3 同样分摊到 B", envelopeDao.isDeletable(envelopeB))
        assertEquals(mapOf(envelopeA to false, envelopeB to false), canDeleteByEnvelope())
    }

    @Test
    fun isDeletable_ignoresSoftDeletedBudgetsAndExpenses() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1, deletedAt = deletedAt)))
        val (garbage, garbageShares) = record(0xB1, LocalDate.of(2026, 10, 5), envelopeA to 6_000, deletedAt = deletedAt)
        seedDao.insertExpenses(listOf(garbage))
        seedDao.insertAllocations(garbageShares)

        assertTrue("垃圾行不构成 RESTRICT 阻挡", envelopeDao.isDeletable(envelopeA))
        assertEquals("汇总查询同样只认存活引用", mapOf(envelopeA to true, envelopeB to true), canDeleteByEnvelope())

        assertEquals(1, expenseDao.clean(envelopeA))
        assertEquals(0, allocationDao.clean(envelopeA))
        assertEquals(1, budgetDao.clean(envelopeA))
        assertEquals(1, envelopeDao.delete(envelopeA))
    }

    @Test
    fun isDeletable_staysFalseAfterGarbageCleanupWhileALiveAllocationRemains() = runBlocking {
        seedMixedRecords()

        assertEquals(1, expenseDao.clean(envelopeA))
        assertEquals(1, allocationDao.clean(envelopeA))

        assertFalse("清掉 B1、B2 的分摊后 B3 仍挡住删除", envelopeDao.isDeletable(envelopeA))
    }

    @Test
    fun isDeletable_ignoresSoftDeletedRowsOfOtherEnvelopes() = runBlocking {
        seedEnvelopes()
        seedDao.insertBudgets(listOf(budget(0xD1, envelopeB, deletedAt = deletedAt)))
        val (garbage, garbageShares) = record(0xB1, LocalDate.of(2026, 10, 6), envelopeB to 3_000, deletedAt = deletedAt)
        seedDao.insertExpenses(listOf(garbage))
        seedDao.insertAllocations(garbageShares)

        assertTrue(envelopeDao.isDeletable(envelopeA))
        assertTrue(envelopeDao.isDeletable(envelopeB))
    }
}
