package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.query.EnvelopeSummaryRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

/**
 * 在真实设备的 SQLite 上复验 EnvelopeDao 汇总查询的主干语义。
 *
 * 与 test 源根的 Robolectric 版本用例对应，用于确认 SQL 行为不依赖 Robolectric 的 SQLite 实现。
 */
@RunWith(AndroidJUnit4::class)
class EnvelopeSummaryQueryInstrumentedTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val month: YearMonth = YearMonth.of(2026, 10)
    private val monthStart: LocalDate = LocalDate.of(2026, 10, 1)
    private val monthEnd: LocalDate = LocalDate.of(2026, 11, 1)
    private val today: LocalDate = LocalDate.of(2026, 10, 20)

    private val envelopeId = EnvelopeId(UUID(0L, 0xA1))

    private lateinit var database: EnvelogDatabase
    private lateinit var dao: EnvelopeDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            EnvelogDatabase::class.java,
        ).build()
        dao = database.envelopeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun at(date: LocalDate): java.time.Instant = date.atTime(LocalTime.NOON).atZone(zone).toInstant()

    private fun envelope(
        isBudgeted: Boolean = true,
        archivedAt: java.time.Instant? = null,
    ): EnvelopeEntity = EnvelopeEntity(envelopeId, "信封", isBudgeted, archivedAt)

    private fun budget(
        value: Long,
        start: LocalDate = monthStart,
        end: LocalDate = monthEnd,
        amount: Long = 100_000,
        attributedMonth: YearMonth = month,
        deletedAt: java.time.Instant? = null,
    ): BudgetEntity = BudgetEntity(
        id = BudgetId(UUID(0L, value)),
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, end),
        attributedMonth = attributedMonth,
        amount = amount,
        deletedAt = deletedAt,
    )

    private fun record(
        value: Long,
        date: LocalDate,
        amount: Long,
        deletedAt: java.time.Instant? = null,
    ): Pair<ExpenditureEntity, ExpenditureAllocationEntity> {
        val id = ExpenditureId(UUID(0L, value))
        val expenditure = ExpenditureEntity(
            id = id,
            happenedAt = at(date),
            happenedDate = date,
            happenedZone = zone,
            deletedAt = deletedAt,
        )
        return expenditure to ExpenditureAllocationEntity(id, envelopeId, amount)
    }

    private suspend fun seed(
        envelopes: List<EnvelopeEntity> = emptyList(),
        budgets: List<BudgetEntity> = emptyList(),
        entries: List<Pair<ExpenditureEntity, ExpenditureAllocationEntity>> = emptyList(),
    ) {
        val seedDao = database.seedDao()
        if (envelopes.isNotEmpty()) seedDao.insertEnvelopes(envelopes)
        if (budgets.isNotEmpty()) seedDao.insertBudgets(budgets)
        if (entries.isNotEmpty()) {
            seedDao.insertExpenditures(entries.map { it.first })
            seedDao.insertAllocations(entries.map { it.second })
        }
    }

    private suspend fun summaries(): List<EnvelopeSummaryRow> = dao.observeEnvelopeSummariesOverlaps(
        today = today,
        yearMonth = month,
        monthStart = monthStart,
        monthEnd = monthEnd,
    ).first()

    @Test
    fun summaries_useMonthWindowWhenEnvelopeHasNoBudget() = runBlocking {
        seed(
            envelopes = listOf(envelope()),
            entries = listOf(
                record(0xB1, LocalDate.of(2026, 10, 5), 10_000),
                record(0xB2, LocalDate.of(2026, 10, 31), 5_000),
                record(0xB3, LocalDate.of(2026, 11, 1), 7_000),
                record(0xB4, LocalDate.of(2026, 9, 30), 3_000),
            ),
        )

        val row = summaries().single()

        assertEquals(15_000L, row.allocated)
        assertNull(row.totalBudget)
        assertNull(row.currentBudget)
        assertNull(row.currentSpanAllocated)
        assertNull(row.priorBudget)
        assertNull(row.priorAllocated)
    }

    @Test
    fun summaries_splitCurrentAndPriorAmounts() = runBlocking {
        seed(
            envelopes = listOf(envelope()),
            budgets = listOf(
                budget(0xD1, start = LocalDate.of(2026, 10, 1), end = LocalDate.of(2026, 10, 15), amount = 40_000),
                budget(0xD2, start = LocalDate.of(2026, 10, 15), end = LocalDate.of(2026, 10, 22), amount = 60_000),
                budget(0xD3, start = LocalDate.of(2026, 10, 22), end = LocalDate.of(2026, 11, 1), amount = 80_000),
            ),
            entries = listOf(
                record(0xB1, LocalDate.of(2026, 10, 5), 12_000),
                record(0xB2, LocalDate.of(2026, 10, 18), 20_000),
                record(0xB3, LocalDate.of(2026, 10, 25), 7_000),
            ),
        )

        val row = summaries().single()

        assertEquals(39_000L, row.allocated)
        assertEquals(180_000L, row.totalBudget)
        assertEquals(60_000L, row.currentBudget)
        assertEquals(LocalDate.of(2026, 10, 15), row.currentSpanStartDate)
        assertEquals(LocalDate.of(2026, 10, 22), row.currentSpanEndDate)
        assertEquals(20_000L, row.currentSpanAllocated)
        assertEquals(40_000L, row.priorBudget)
        assertEquals(12_000L, row.priorAllocated)
    }

    @Test
    fun summaries_ignoreSoftDeletedRows() = runBlocking {
        val deletedAt = java.time.Instant.parse("2026-10-19T04:00:00Z")
        seed(
            envelopes = listOf(envelope()),
            budgets = listOf(
                budget(0xD1, amount = 100_000),
                budget(0xD2, amount = 50_000, deletedAt = deletedAt),
            ),
            entries = listOf(
                record(0xB1, LocalDate.of(2026, 10, 10), 10_000),
                record(0xB2, LocalDate.of(2026, 10, 11), 20_000, deletedAt = deletedAt),
            ),
        )

        val row = summaries().single()

        assertEquals(10_000L, row.allocated)
        assertEquals(100_000L, row.totalBudget)
        assertEquals(10_000L, row.currentSpanAllocated)
    }

    @Test
    fun summaries_countOnlyAttributedMonthInTotalBudget() = runBlocking {
        seed(
            envelopes = listOf(envelope()),
            budgets = listOf(budget(0xD1, amount = 70_000, attributedMonth = month.minusMonths(1))),
            entries = listOf(record(0xB1, LocalDate.of(2026, 10, 5), 10_000)),
        )

        val row = summaries().single()

        assertEquals(10_000L, row.allocated)
        assertNull(row.totalBudget)
        assertEquals(70_000L, row.currentBudget)
        assertEquals(monthStart, row.currentSpanStartDate)
        assertEquals(monthEnd, row.currentSpanEndDate)
        assertEquals(10_000L, row.currentSpanAllocated)
    }
}
