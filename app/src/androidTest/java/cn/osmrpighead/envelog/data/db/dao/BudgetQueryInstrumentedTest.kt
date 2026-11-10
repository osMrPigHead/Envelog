package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * 在真实设备的 SQLite 上复验预算区间重叠计数。
 */
@RunWith(AndroidJUnit4::class)
class BudgetQueryInstrumentedTest {
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)

    private val envelopeA = EnvelopeId(UUID(0L, 0xA1))
    private val envelopeB = EnvelopeId(UUID(0L, 0xA2))

    private lateinit var database: EnvelogDatabase
    private lateinit var dao: BudgetDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnvelogDatabase::class.java,
        ).build()
        dao = database.budgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun envelope(id: EnvelopeId, name: String, isBudgeted: Boolean = true): EnvelopeEntity =
        EnvelopeEntity(id, name, isBudgeted, archivedAt = null)

    private fun budget(
        value: Long,
        envelopeId: EnvelopeId = envelopeA,
        start: LocalDate,
        end: LocalDate,
        amount: Long = 100_000,
    ): BudgetEntity = BudgetEntity(
        id = BudgetId(UUID(0L, value)),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, end),
        attributedMonth = month,
        amount = amount,
        deletedAt = null,
    )

    private suspend fun seed(budgets: List<BudgetEntity>) {
        val seedDao = database.seedDao()
        seedDao.insertEnvelopes(
            listOf(
                envelope(envelopeA, "餐饮"),
                envelope(envelopeB, "购物", isBudgeted = false),
            ),
        )
        seedDao.insertBudgets(budgets)
    }

    private suspend fun overlaps(
        budgetId: BudgetId = BudgetId(UUID(0L, 0xEF)),
    ): Long = dao.countOverlaps(envelopeA, budgetId, monthStart, monthEnd)

    @Test
    fun countOverlaps_countsBudgetsCrossingOrInsideSpan() = runBlocking {
        val wholeMonth = budget(0xD1, start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 11, 1))
        val crossesStart = budget(0xD2, start = LocalDate.of(2026, 9, 20), end = LocalDate.of(2026, 10, 5))
        val inside = budget(0xD3, start = LocalDate.of(2026, 10, 10), end = LocalDate.of(2026, 10, 12))
        val startsAtSpanEnd = budget(0xD4, start = LocalDate.of(2026, 11, 1), end = LocalDate.of(2026, 12, 1))
        val otherEnvelope = budget(
            0xD5,
            envelopeId = envelopeB,
            start = LocalDate.of(2026, 10, 1),
            end = LocalDate.of(2026, 11, 1),
        )

        seed(listOf(wholeMonth, crossesStart, inside, startsAtSpanEnd, otherEnvelope))

        assertEquals(3L, overlaps())
    }

    @Test
    fun countOverlaps_returnsZeroWhenEveryBudgetStartsAfterSpan() = runBlocking {
        seed(
            listOf(
                budget(0xD1, start = LocalDate.of(2026, 11, 1), end = LocalDate.of(2026, 12, 1)),
            ),
        )

        assertEquals(0L, overlaps())
    }

    @Test
    fun countOverlaps_excludesGivenBudgetId() = runBlocking {
        seed(
            listOf(
                budget(0xD1, start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 11, 1)),
            ),
        )

        assertEquals(0L, overlaps(BudgetId(UUID(0L, 0xD1))))
        assertEquals(1L, overlaps(BudgetId(UUID(0L, 0xD2))))
    }
}
