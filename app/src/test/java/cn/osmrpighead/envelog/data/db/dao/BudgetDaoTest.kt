package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
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
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class BudgetDaoTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var dao: BudgetDao

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        dao = database.budgetDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun overlaps(
        envelopeId: EnvelopeId = RoomTestData.envelopeA,
        budgetId: BudgetId = RoomTestData.budgetId(0xEE),
        spanStart: LocalDate = RoomTestData.monthStart,
        spanEnd: LocalDate = RoomTestData.monthEnd,
    ): Long = dao.countOverlaps(envelopeId, budgetId, spanStart, spanEnd)

    @Test
    fun countOverlaps_countsBudgetsCrossingOrInsideSpan() = runTest {
        val wholeMonth = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = LocalDate.of(2026, 10, 1),
            end = LocalDate.of(2026, 11, 1),
        )
        val crossesStart = RoomTestData.budget(
            id = RoomTestData.budgetB,
            start = LocalDate.of(2026, 9, 20),
            end = LocalDate.of(2026, 10, 5),
        )
        val inside = RoomTestData.budget(
            id = RoomTestData.budgetC,
            start = LocalDate.of(2026, 10, 10),
            end = LocalDate.of(2026, 10, 12),
        )
        val startsAtSpanEnd = RoomTestData.budget(
            id = RoomTestData.budgetId(0xD4),
            start = LocalDate.of(2026, 11, 1),
            end = LocalDate.of(2026, 12, 1),
        )
        val otherEnvelope = RoomTestData.budget(
            id = RoomTestData.budgetId(0xD5),
            envelopeId = RoomTestData.envelopeB,
            start = LocalDate.of(2026, 10, 1),
            end = LocalDate.of(2026, 11, 1),
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(wholeMonth, crossesStart, inside, startsAtSpanEnd, otherEnvelope),
        )

        assertEquals(3L, overlaps())
    }

    @Test
    fun countOverlaps_filtersByEnvelope() = runTest {
        val ofA = RoomTestData.budget(id = RoomTestData.budgetA, envelopeId = RoomTestData.envelopeA)
        val ofB = RoomTestData.budget(id = RoomTestData.budgetB, envelopeId = RoomTestData.envelopeB)

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(ofA, ofB),
        )

        assertEquals(1L, overlaps(RoomTestData.envelopeA))
        assertEquals(1L, overlaps(RoomTestData.envelopeB))
    }

    @Test
    fun countOverlaps_returnsZeroWhenEveryBudgetStartsAfterSpan() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    start = LocalDate.of(2026, 11, 1),
                    end = LocalDate.of(2026, 12, 1),
                ),
            ),
        )

        assertEquals(0L, overlaps())
    }

    @Test
    fun countOverlaps_excludesBudgetsEndingAtOrBeforeSpanStart() = runTest {
        val endedBefore = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = LocalDate.of(2026, 9, 1),
            end = LocalDate.of(2026, 10, 1),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(endedBefore),
        )

        assertEquals(0L, overlaps())
    }

    @Test
    fun countOverlaps_excludesGivenBudgetId() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(RoomTestData.budget(id = RoomTestData.budgetA)),
        )

        assertEquals(0L, overlaps(budgetId = RoomTestData.budgetA))
        assertEquals(1L, overlaps(budgetId = RoomTestData.budgetB))
    }

    @Test
    fun countOverlaps_excludesSoftDeletedBudgets() = runTest {
        val deleted = RoomTestData.budget(
            id = RoomTestData.budgetA,
            deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(deleted),
        )

        assertEquals(0L, overlaps())
    }

    private suspend fun attributedMonth(
        envelopeId: EnvelopeId = RoomTestData.envelopeA,
        month: YearMonth = RoomTestData.month,
    ): List<BudgetEntity> = dao.observe(envelopeId, month).first()

    @Test
    fun observeBudgetAttributedMonth_filtersByEnvelopeAndMonth() = runTest {
        val octoberOfA = RoomTestData.budget(
            id = RoomTestData.budgetA,
            envelopeId = RoomTestData.envelopeA,
        )
        val septemberOfA = RoomTestData.budget(
            id = RoomTestData.budgetB,
            envelopeId = RoomTestData.envelopeA,
            start = LocalDate.of(2026, 9, 1),
            end = LocalDate.of(2026, 10, 1),
            attributedMonth = RoomTestData.month.minusMonths(1),
        )
        val octoberOfB = RoomTestData.budget(
            id = RoomTestData.budgetC,
            envelopeId = RoomTestData.envelopeB,
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(octoberOfA, septemberOfA, octoberOfB),
        )

        assertEquals(listOf(octoberOfA.id), attributedMonth().map { it.id })
    }

    @Test
    fun observeBudgetAttributedMonth_excludesSoftDeletedBudgets() = runTest {
        val deleted = RoomTestData.budget(
            id = RoomTestData.budgetA,
            deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(deleted),
        )

        assertTrue("软删除的预算不应按归属月份返回", attributedMonth().isEmpty())
    }

    @Test
    fun observeBudgetAttributedMonth_usesAttributionNotSpanMonths() = runTest {
        val septemberAttributed = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = LocalDate.of(2026, 9, 20),
            end = LocalDate.of(2026, 10, 5),
            attributedMonth = RoomTestData.month.minusMonths(1),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(septemberAttributed),
        )

        assertEquals(
            listOf(septemberAttributed.id),
            attributedMonth(month = RoomTestData.month.minusMonths(1)).map { it.id },
        )
        assertTrue(
            "预算区间跨到 10 月但归属 9 月，不应出现在 10 月的归属查询中",
            attributedMonth(month = RoomTestData.month).isEmpty(),
        )
    }

    @Test
    fun clean_deletesSoftDeletedBudgetsOfTheEnvelopeOnly() = runTest {
        val deletedOfA = RoomTestData.budget(
            id = RoomTestData.budgetA,
            envelopeId = RoomTestData.envelopeA,
            deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
        )
        val liveOfA = RoomTestData.budget(
            id = RoomTestData.budgetB,
            envelopeId = RoomTestData.envelopeA,
        )
        val deletedOfB = RoomTestData.budget(
            id = RoomTestData.budgetC,
            envelopeId = RoomTestData.envelopeB,
            deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(deletedOfA, liveOfA, deletedOfB),
        )

        assertEquals(1, dao.clean(RoomTestData.envelopeA))
        assertEquals("已清理的预算行应被物理删除", 0, dao.clean(RoomTestData.envelopeA))
        assertEquals(1L, overlaps())
        assertEquals("其他信封的软删除预算不受影响", 1, dao.clean(RoomTestData.envelopeB))
    }

    @Test
    fun clean_returnsZeroWhenTheEnvelopeHasNoSoftDeletedBudget() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(RoomTestData.budget(id = RoomTestData.budgetA)),
        )

        assertEquals(0, dao.clean(RoomTestData.envelopeA))
        assertEquals(1L, overlaps())
    }

    @Test
    fun clean_returnsZeroForUnknownEnvelope() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    id = RoomTestData.budgetA,
                    deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
                ),
            ),
        )

        assertEquals(0, dao.clean(RoomTestData.envelopeId(0xEE)))
    }
}
