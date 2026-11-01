package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.query.EnvelopeSummaryRow
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
class EnvelopeDaoTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var dao: EnvelopeDao

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        dao = database.envelopeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun summaries(): List<EnvelopeSummaryRow> =
        dao.observeEnvelopeSummariesOverlaps(
            today = RoomTestData.today,
            yearMonth = RoomTestData.month,
            monthStart = RoomTestData.monthStart,
            monthEnd = RoomTestData.monthEnd,
        ).first()

    @Test
    fun observeEnvelopeSummaries_usesMonthWindowWhenEnvelopeHasNoBudget() = runTest {
        val (inside, insideShares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 5), RoomTestData.envelopeA to 10_000)
        val (lastDay, lastDayShares) = RoomTestData.record(0xB2, LocalDate.of(2026, 10, 31), RoomTestData.envelopeA to 5_000)
        val (nextMonth, nextMonthShares) = RoomTestData.record(0xB3, LocalDate.of(2026, 11, 1), RoomTestData.envelopeA to 7_000)
        val (previousMonth, previousMonthShares) = RoomTestData.record(0xB4, LocalDate.of(2026, 9, 30), RoomTestData.envelopeA to 3_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            expenditures = listOf(inside, lastDay, nextMonth, previousMonth),
            allocations = insideShares + lastDayShares + nextMonthShares + previousMonthShares,
        )

        val row = summaries().single()

        assertEquals(RoomTestData.envelopeA, row.id)
        assertEquals(15_000L, row.allocated)
        assertNull(row.totalBudget)
        assertNull(row.currentBudget)
        assertNull(row.currentSpanStartDate)
        assertNull(row.currentSpanEndDate)
        assertNull(row.currentSpanAllocated)
        assertNull(row.priorBudget)
        // 没有当前预算时，历史窗口的终点回退到 today，因此今天之前的支出都算历史支出
        assertEquals(10_000L, row.priorAllocated)
    }

    @Test
    fun observeEnvelopeSummaries_extendsWindowToSpanOfAttributedMonthBudget() = runTest {
        val (beforeMonth, beforeMonthShares) = RoomTestData.record(0xB1, LocalDate.of(2026, 9, 25), RoomTestData.envelopeA to 10_000)
        val (beforeSpan, beforeSpanShares) = RoomTestData.record(0xB2, LocalDate.of(2026, 9, 19), RoomTestData.envelopeA to 4_000)
        val (afterSpan, afterSpanShares) = RoomTestData.record(0xB3, LocalDate.of(2026, 10, 6), RoomTestData.envelopeA to 2_000)
        val (afterMonth, afterMonthShares) = RoomTestData.record(0xB4, LocalDate.of(2026, 11, 2), RoomTestData.envelopeA to 5_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    start = LocalDate.of(2026, 9, 20),
                    end = LocalDate.of(2026, 10, 5),
                    amount = 60_000,
                ),
            ),
            expenditures = listOf(beforeMonth, beforeSpan, afterSpan, afterMonth),
            allocations = beforeMonthShares + beforeSpanShares + afterSpanShares + afterMonthShares,
        )

        val row = summaries().single()

        assertEquals(12_000L, row.allocated)
        assertEquals(60_000L, row.totalBudget)
        assertNull(row.currentBudget)
        assertNull(row.currentSpanAllocated)
        assertEquals(60_000L, row.priorBudget)
        assertEquals(12_000L, row.priorAllocated)
    }

    @Test
    fun observeEnvelopeSummaries_splitsCurrentAndPriorAmounts() = runTest {
        val (first, firstShares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 5), RoomTestData.envelopeA to 12_000)
        val (second, secondShares) = RoomTestData.record(0xB2, LocalDate.of(2026, 10, 18), RoomTestData.envelopeA to 20_000)
        val (third, thirdShares) = RoomTestData.record(0xB3, LocalDate.of(2026, 10, 25), RoomTestData.envelopeA to 7_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    id = RoomTestData.budgetA,
                    start = LocalDate.of(2026, 10, 1),
                    end = LocalDate.of(2026, 10, 15),
                    amount = 40_000,
                ),
                RoomTestData.budget(
                    id = RoomTestData.budgetB,
                    start = LocalDate.of(2026, 10, 15),
                    end = LocalDate.of(2026, 10, 22),
                    amount = 60_000,
                ),
                RoomTestData.budget(
                    id = RoomTestData.budgetC,
                    start = LocalDate.of(2026, 10, 22),
                    end = LocalDate.of(2026, 11, 1),
                    amount = 80_000,
                ),
            ),
            expenditures = listOf(first, second, third),
            allocations = firstShares + secondShares + thirdShares,
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
    fun observeEnvelopeSummaries_ignoresSoftDeletedBudgetsAndExpenditures() = runTest {
        val (live, liveShares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 10), RoomTestData.envelopeA to 10_000)
        val (deleted, deletedShares) = RoomTestData.record(
            0xB2,
            LocalDate.of(2026, 10, 11),
            RoomTestData.envelopeA to 20_000,
        )
        val deletedAt = Instant.parse("2026-10-19T04:00:00Z")

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(id = RoomTestData.budgetA, amount = 100_000),
                RoomTestData.budget(id = RoomTestData.budgetB, amount = 50_000, deletedAt = deletedAt),
            ),
            expenditures = listOf(live, deleted.copy(deletedAt = deletedAt)),
            allocations = liveShares + deletedShares,
        )

        val row = summaries().single()

        assertEquals(10_000L, row.allocated)
        assertEquals(100_000L, row.totalBudget)
        assertEquals(100_000L, row.currentBudget)
        assertEquals(10_000L, row.currentSpanAllocated)
        assertNull(row.priorBudget)
        assertNull(row.priorAllocated)
    }

    @Test
    fun observeEnvelopeSummaries_countsOnlyAttributedMonthInTotalBudget() = runTest {
        val (expenditure, shares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 5), RoomTestData.envelopeA to 10_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    amount = 70_000,
                    attributedMonth = RoomTestData.month.minusMonths(1),
                ),
            ),
            expenditures = listOf(expenditure),
            allocations = shares,
        )

        val row = summaries().single()

        assertEquals(10_000L, row.allocated)
        assertNull(row.totalBudget)
        assertEquals(70_000L, row.currentBudget)
        assertEquals(RoomTestData.monthStart, row.currentSpanStartDate)
        assertEquals(RoomTestData.monthEnd, row.currentSpanEndDate)
        assertEquals(10_000L, row.currentSpanAllocated)
        assertNull(row.priorBudget)
        assertNull(row.priorAllocated)
    }

    @Test
    fun archiveEnvelopeById_setsArchivedAtOnlyOnce() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val first = Instant.parse("2026-10-20T04:00:00Z")
        val second = Instant.parse("2026-10-21T04:00:00Z")

        assertEquals(1, dao.archiveEnvelopeById(RoomTestData.envelopeA, first))
        assertEquals(0, dao.archiveEnvelopeById(RoomTestData.envelopeA, second))
        assertEquals(first, dao.getById(RoomTestData.envelopeA)?.archivedAt)
    }

    @Test
    fun getByIds_returnsOnlyExistingEnvelopes() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA, name = "餐饮"),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
        )

        val found = dao.getByIds(setOf(RoomTestData.envelopeA, RoomTestData.envelopeId(0xEE), RoomTestData.envelopeB))

        assertEquals(
            setOf(RoomTestData.envelopeA, RoomTestData.envelopeB),
            found.map { it.id }.toSet(),
        )
    }

    @Test
    fun getById_returnsNullForUnknownEnvelope() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertNull(dao.getById(RoomTestData.envelopeId(0xEE)))
    }

    @Test
    fun observeEnvelopeSummaries_returnsIndependentRowsPerEnvelope() = runTest {
        val (aExpenditure, aShares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 5), RoomTestData.envelopeA to 6_000)
        val (bExpenditure, bShares) = RoomTestData.record(0xB2, LocalDate.of(2026, 10, 25), RoomTestData.envelopeB to 4_000)

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(RoomTestData.budget(envelopeId = RoomTestData.envelopeA, amount = 100_000)),
            expenditures = listOf(aExpenditure, bExpenditure),
            allocations = aShares + bShares,
        )

        val rows = summaries().associateBy { it.id }

        assertEquals(setOf(RoomTestData.envelopeA, RoomTestData.envelopeB), rows.keys)
        val budgeted = rows.getValue(RoomTestData.envelopeA)
        assertEquals(6_000L, budgeted.allocated)
        assertEquals(100_000L, budgeted.totalBudget)
        assertEquals(100_000L, budgeted.currentBudget)
        assertEquals(6_000L, budgeted.currentSpanAllocated)
        assertNull(budgeted.priorAllocated)

        val nonBudgeted = rows.getValue(RoomTestData.envelopeB)
        assertEquals(4_000L, nonBudgeted.allocated)
        assertNull(nonBudgeted.totalBudget)
        assertNull(nonBudgeted.currentBudget)
        assertNull(nonBudgeted.priorAllocated)
    }

    @Test
    fun observeEnvelopeSummaries_returnsEmptyListWhenDatabaseIsEmpty() = runTest {
        assertTrue(summaries().isEmpty())
    }

    @Test
    fun getByIds_returnsEmptyListWhenDatabaseIsEmpty() = runTest {
        assertTrue(dao.getByIds(setOf(RoomTestData.envelopeA)).isEmpty())
    }

    @Test
    fun archiveEnvelopeById_returnsZeroWhenEnvelopeIsMissing() = runTest {
        assertEquals(0, dao.archiveEnvelopeById(RoomTestData.envelopeA, Instant.parse("2026-10-20T04:00:00Z")))
    }

    @Test
    fun getById_returnsArchivedEnvelope() = runTest {
        val archivedAt = Instant.parse("2026-10-01T04:00:00Z")
        database.seed(envelopes = listOf(RoomTestData.envelope(archivedAt = archivedAt)))

        assertEquals(archivedAt, dao.getById(RoomTestData.envelopeA)?.archivedAt)
    }

    @Test
    fun getByIds_includesArchivedEnvelopes() = runTest {
        val archivedAt = Instant.parse("2026-10-01T04:00:00Z")
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "旧信封", isBudgeted = false, archivedAt = archivedAt),
            ),
        )

        val found = dao.getByIds(setOf(RoomTestData.envelopeA, RoomTestData.envelopeB))

        assertEquals(2, found.size)
        assertNull(found.single { it.id == RoomTestData.envelopeA }.archivedAt)
        assertEquals(archivedAt, found.single { it.id == RoomTestData.envelopeB }.archivedAt)
    }
}
