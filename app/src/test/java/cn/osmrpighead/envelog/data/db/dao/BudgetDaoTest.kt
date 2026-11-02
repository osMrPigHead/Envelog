package cn.osmrpighead.envelog.data.db.dao

import android.app.Application
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
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
        envelopeId: cn.osmrpighead.envelog.data.model.EnvelopeId = RoomTestData.envelopeA,
        spanStart: LocalDate = RoomTestData.monthStart,
        spanEnd: LocalDate = RoomTestData.monthEnd,
    ): List<BudgetEntity> = dao.observeBudgetOverlaps(envelopeId, spanStart, spanEnd).first()

    @Test
    fun observeBudgetOverlaps_returnsBudgetsCrossingOrInsideSpan() = runTest {
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

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(wholeMonth, crossesStart, inside, startsAtSpanEnd),
        )

        assertEquals(
            setOf(wholeMonth.id, crossesStart.id, inside.id),
            overlaps().map { it.id }.toSet(),
        )
    }

    @Test
    fun observeBudgetOverlaps_filtersByEnvelope() = runTest {
        val ofA = RoomTestData.budget(id = RoomTestData.budgetA, envelopeId = RoomTestData.envelopeA)
        val ofB = RoomTestData.budget(id = RoomTestData.budgetB, envelopeId = RoomTestData.envelopeB)

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            budgets = listOf(ofA, ofB),
        )

        assertEquals(listOf(ofA.id), overlaps(RoomTestData.envelopeA).map { it.id })
        assertEquals(listOf(ofB.id), overlaps(RoomTestData.envelopeB).map { it.id })
    }

    @Test
    fun observeBudgetOverlaps_returnsEmptyWhenEveryBudgetStartsAfterSpan() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    start = LocalDate.of(2026, 11, 1),
                    end = LocalDate.of(2026, 12, 1),
                ),
            ),
        )

        assertTrue(overlaps().isEmpty())
    }

    @Test
    fun observeBudgetOverlaps_excludesBudgetsEndingAtOrBeforeSpanStart() = runTest {
        val endedBefore = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = LocalDate.of(2026, 9, 1),
            end = LocalDate.of(2026, 10, 1),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(endedBefore),
        )

        assertTrue("完全不与区间重叠的预算不应返回，实际为 ${overlaps()}", overlaps().isEmpty())
    }

    @Test
    fun observeBudgetOverlaps_excludesSoftDeletedBudgets() = runTest {
        val deleted = RoomTestData.budget(
            id = RoomTestData.budgetA,
            deletedAt = Instant.parse("2026-10-19T04:00:00Z"),
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(deleted),
        )

        assertTrue("软删除的预算不应返回，实际为 ${overlaps()}", overlaps().isEmpty())
    }
}
