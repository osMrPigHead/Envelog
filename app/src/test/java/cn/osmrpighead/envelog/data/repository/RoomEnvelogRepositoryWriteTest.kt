package cn.osmrpighead.envelog.data.repository

import android.app.Application
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.repository.room.RoomEnvelogRepository
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomEnvelogRepositoryWriteTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var repository: RoomEnvelogRepository

    @Before
    fun setUp() {
        database = RoomTestData.createDatabase()
        repository = RoomEnvelogRepository(
            db = database,
            clock = Clock.fixed(RoomTestData.at(RoomTestData.today), RoomTestData.zone),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun date(month: Int, day: Int): LocalDate = LocalDate.of(2026, month, day)

    private fun envelope(
        id: EnvelopeId = RoomTestData.envelopeA,
        name: String = "餐饮",
        isBudgeted: Boolean = true,
        archived: Boolean = false,
    ): Envelope = Envelope(id = id, name = name, isBudgeted = isBudgeted, archived = archived)

    private fun budget(
        id: BudgetId,
        envelopeId: EnvelopeId = RoomTestData.envelopeA,
        start: LocalDate = date(10, 1),
        end: LocalDate = date(11, 1),
        attributedMonth: YearMonth = RoomTestData.month,
        amount: Long = 100_000,
    ): Budget = Budget(
        id = id,
        envelopeId = envelopeId,
        dateSpan = HalfOpenDateSpan(start, end),
        attributedMonth = attributedMonth,
        amount = amount,
    )

    private suspend fun budgetsOf(month: YearMonth = RoomTestData.month): List<Budget> =
        repository.observeBudgets(RoomTestData.envelopeA, month).first()

    @Test
    fun getEnvelopeById_mapsStoredEnvelope() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA, name = "餐饮"),
                RoomTestData.envelope(
                    RoomTestData.envelopeB,
                    name = "旧信封",
                    archivedAt = Instant.parse("2026-09-01T04:00:00Z"),
                ),
            ),
        )

        assertEquals(
            Envelope(RoomTestData.envelopeA, "餐饮", true, false),
            repository.getEnvelope(RoomTestData.envelopeA),
        )
        assertEquals(
            Envelope(RoomTestData.envelopeB, "旧信封", true, true),
            repository.getEnvelope(RoomTestData.envelopeB),
        )
    }

    @Test
    fun getEnvelopeById_throwsForMissingEnvelope() = runTest {
        val error = runCatching {
            repository.getEnvelope(RoomTestData.envelopeId(0xEE))
        }.exceptionOrNull()

        assertTrue("缺少信封时应抛 NoSuchElementException，实际为 $error", error is NoSuchElementException)
    }

    @Test
    fun insertEnvelope_persistsEnvelopeAndBudgets() = runTest {
        val firstHalf = budget(
            id = RoomTestData.budgetA,
            start = date(10, 1),
            end = date(10, 15),
            amount = 100_000,
        )
        val secondHalf = budget(
            id = RoomTestData.budgetB,
            start = date(10, 15),
            end = date(10, 22),
            amount = 50_000,
        )

        assertEquals(
            BudgetWriteResult.Success,
            repository.insertEnvelope(envelope(), listOf(firstHalf, secondHalf)),
        )

        assertEquals(envelope(), repository.getEnvelope(RoomTestData.envelopeA))
        assertEquals(setOf(firstHalf, secondHalf), budgetsOf().toSet())
    }

    @Test
    fun insertEnvelope_rejectsOverlappingBudgets() = runTest {
        val first = budget(
            id = RoomTestData.budgetA,
            start = date(10, 1),
            end = date(10, 15),
            amount = 100_000,
        )
        val overlapping = budget(
            id = RoomTestData.budgetB,
            start = date(10, 10),
            end = date(10, 20),
            amount = 50_000,
        )

        assertEquals(
            BudgetWriteResult.BudgetOverlaps,
            repository.insertEnvelope(envelope(), listOf(first, overlapping)),
        )

        assertNull(database.envelopeDao().get(RoomTestData.envelopeA))
        assertTrue(budgetsOf().isEmpty())
    }

    @Test
    fun insertEnvelope_withEmptyBudgets_persistsEnvelopeOnly() = runTest {
        assertEquals(
            BudgetWriteResult.Success,
            repository.insertEnvelope(envelope(), emptyList()),
        )

        assertEquals(envelope(), repository.getEnvelope(RoomTestData.envelopeA))
        assertTrue(budgetsOf().isEmpty())
    }

    @Test
    fun updateEnvelope_overwritesEditableFields() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA, name = "餐饮"),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
        )

        repository.updateEnvelope(Envelope(RoomTestData.envelopeA, "外食", false, false))

        assertEquals(
            Envelope(RoomTestData.envelopeA, "外食", false, false),
            repository.getEnvelope(RoomTestData.envelopeA),
        )
        assertEquals(
            Envelope(RoomTestData.envelopeB, "购物", false, false),
            repository.getEnvelope(RoomTestData.envelopeB),
        )
    }

    @Test
    fun updateEnvelope_ofMissingEnvelope_isNoOp() = runTest {
        val missing = RoomTestData.envelopeId(0xEE)

        repository.updateEnvelope(Envelope(missing, "不存在", false, false))

        assertNull(database.envelopeDao().get(missing))
    }

    @Test
    fun insertBudget_persistsWithNormalizedAttribution() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val staleAttribution = budget(
            id = RoomTestData.budgetA,
            start = date(9, 20),
            end = date(10, 5),
            attributedMonth = RoomTestData.month,
            amount = 70_000,
        )

        assertEquals(BudgetWriteResult.Success, repository.insertBudget(staleAttribution))

        assertEquals(
            listOf(staleAttribution.copy(attributedMonth = RoomTestData.month.minusMonths(1))),
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month.minusMonths(1)).first(),
        )
        assertTrue(
            "归属月份应按跨度重新计算为 9 月，10 月查询应为空",
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month).first().isEmpty(),
        )
    }

    @Test
    fun insertBudget_rejectsOverlappingBudget() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    id = RoomTestData.budgetA,
                    start = date(10, 1),
                    end = date(10, 20),
                ),
            ),
        )
        val overlapping = budget(
            id = RoomTestData.budgetB,
            start = date(10, 10),
            end = date(10, 25),
            amount = 50_000,
        )

        assertEquals(BudgetWriteResult.BudgetOverlaps, repository.insertBudget(overlapping))
        assertEquals(1, budgetsOf().size)
        assertEquals(RoomTestData.budgetA, budgetsOf().single().id)
    }

    @Test
    fun insertBudget_rejectsArchivedEnvelope() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(
                    archivedAt = Instant.parse("2026-09-01T04:00:00Z"),
                ),
            ),
        )

        assertEquals(
            BudgetWriteResult.EnvelopeArchived,
            repository.insertBudget(budget(RoomTestData.budgetA)),
        )
        assertTrue(budgetsOf().isEmpty())
    }

    @Test
    fun updateBudget_rejectsOverlappingBudget() = runTest {
        val first = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = date(10, 1),
            end = date(10, 5),
        )
        val second = RoomTestData.budget(
            id = RoomTestData.budgetB,
            start = date(10, 10),
            end = date(10, 15),
        )
        database.seed(envelopes = listOf(RoomTestData.envelope()), budgets = listOf(first, second))

        val overlapping = budget(
            id = RoomTestData.budgetA,
            start = date(10, 8),
            end = date(10, 12),
            amount = 80_000,
        )

        assertEquals(BudgetWriteResult.BudgetOverlaps, repository.updateBudget(overlapping))
        assertEquals(setOf(first.id, second.id), budgetsOf().map { it.id }.toSet())
        assertEquals(100_000L, budgetsOf().single { it.id == RoomTestData.budgetA }.amount)
    }

    @Test
    fun updateBudget_rejectsArchivedEnvelope() = runTest {
        val existing = RoomTestData.budget(id = RoomTestData.budgetA)
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(
                    archivedAt = Instant.parse("2026-09-01T04:00:00Z"),
                ),
            ),
            budgets = listOf(existing),
        )

        assertEquals(
            BudgetWriteResult.EnvelopeArchived,
            repository.updateBudget(
                Budget(
                    id = existing.id,
                    envelopeId = existing.envelopeId,
                    dateSpan = existing.dateSpan,
                    attributedMonth = existing.attributedMonth,
                    amount = 120_000,
                ),
            ),
        )
        assertEquals(listOf(existing.id), budgetsOf().map { it.id })
        assertEquals(100_000L, budgetsOf().single().amount)
    }

    @Test
    fun updateBudget_persistsAmountChangeWithoutMovingSpan() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val initial = budget(
            id = RoomTestData.budgetA,
            start = date(10, 1),
            end = date(10, 15),
            amount = 70_000,
        )
        repository.insertBudget(initial)
        val updated = initial.copy(amount = 120_000)

        assertEquals(BudgetWriteResult.Success, repository.updateBudget(updated))
        assertEquals(listOf(updated), budgetsOf())
    }

    @Test
    fun updateBudget_persistsUpdatedAmountAndAttribution() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val initial = budget(
            id = RoomTestData.budgetA,
            start = date(9, 20),
            end = date(10, 5),
            attributedMonth = RoomTestData.month.minusMonths(1),
            amount = 70_000,
        )
        repository.insertBudget(initial)
        val updated = initial.copy(
            dateSpan = HalfOpenDateSpan(date(10, 10), date(10, 20)),
            attributedMonth = RoomTestData.month.minusMonths(1),
            amount = 120_000,
        )

        repository.updateBudget(updated)

        assertEquals(
            listOf(updated.copy(attributedMonth = RoomTestData.month)),
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month).first(),
        )
        assertTrue(
            "更新后归属月份应重新计算为 10 月，9 月查询应为空",
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month.minusMonths(1)).first()
                .isEmpty(),
        )
    }

    @Test
    fun replaceBudgets_deletesConflictsAndInsertsNewBudgets() = runTest {
        val deleted = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = date(10, 1),
            end = date(10, 21),
            amount = 100_000,
        )
        val keptEntity = RoomTestData.budget(
            id = RoomTestData.budgetB,
            start = date(10, 25),
            end = date(10, 28),
            amount = 50_000,
        )
        val kept = budget(
            id = RoomTestData.budgetB,
            start = date(10, 25),
            end = date(10, 28),
            amount = 50_000,
        )
        database.seed(envelopes = listOf(RoomTestData.envelope()), budgets = listOf(deleted, keptEntity))
        val first = budget(
            id = RoomTestData.budgetC,
            start = date(10, 20),
            end = date(10, 22),
            amount = 60_000,
        )
        val second = budget(
            id = RoomTestData.budgetId(0xD4),
            start = date(10, 22),
            end = date(10, 25),
            amount = 70_000,
        )

        assertEquals(
            BudgetWriteResult.Success,
            repository.replaceBudgets(RoomTestData.envelopeA, listOf(RoomTestData.budgetA), listOf(first, second)),
        )

        assertEquals(setOf(kept, first, second), budgetsOf().toSet())
    }

    @Test
    fun replaceBudgets_withoutDeletesInsertsNewBudgets() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val newBudget = budget(
            id = RoomTestData.budgetA,
            start = date(10, 20),
            end = date(10, 22),
            amount = 50_000,
        )

        assertEquals(
            BudgetWriteResult.Success,
            repository.replaceBudgets(RoomTestData.envelopeA, emptyList(), listOf(newBudget)),
        )

        assertEquals(listOf(newBudget), budgetsOf())
    }

    @Test
    fun replaceBudgets_rejectsArchivedEnvelope() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(archivedAt = Instant.parse("2026-09-01T04:00:00Z")),
            ),
            budgets = listOf(RoomTestData.budget(id = RoomTestData.budgetA)),
        )

        assertEquals(
            BudgetWriteResult.EnvelopeArchived,
            repository.replaceBudgets(
                RoomTestData.envelopeA,
                emptyList(),
                listOf(budget(RoomTestData.budgetB, start = date(10, 20), end = date(10, 22))),
            ),
        )
        assertEquals(listOf(budget(RoomTestData.budgetA)), budgetsOf())
    }

    @Test
    fun replaceBudgets_rejectsOverlapWithBudgetNotDeleted() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    id = RoomTestData.budgetA,
                    start = date(10, 15),
                    end = date(10, 25),
                    amount = 100_000,
                ),
            ),
        )

        assertEquals(
            BudgetWriteResult.BudgetOverlaps,
            repository.replaceBudgets(
                RoomTestData.envelopeA,
                emptyList(),
                listOf(budget(RoomTestData.budgetB, start = date(10, 20), end = date(10, 22))),
            ),
        )
        assertEquals(
            listOf(budget(RoomTestData.budgetA, start = date(10, 15), end = date(10, 25))),
            budgetsOf(),
        )
    }

    @Test
    fun replaceBudgets_rejectsSelfOverlappingNewBudgets() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))
        val first = budget(RoomTestData.budgetA, start = date(10, 20), end = date(10, 25))
        val second = budget(RoomTestData.budgetB, start = date(10, 22), end = date(10, 28))

        assertEquals(
            BudgetWriteResult.BudgetOverlaps,
            repository.replaceBudgets(RoomTestData.envelopeA, emptyList(), listOf(first, second)),
        )
        assertTrue(budgetsOf().isEmpty())
    }

    @Test
    fun deleteBudget_deletesOnceThenReportsNotFoundOrAlreadyDeleted() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(RoomTestData.budget(id = RoomTestData.budgetA)),
        )

        assertEquals(DeleteResult.Deleted, repository.deleteBudget(RoomTestData.budgetA))
        assertTrue(
            "删除后预算不应再按归属月份返回",
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month).first().isEmpty(),
        )
        assertEquals(
            DeleteResult.NotFoundOrAlreadyDeleted,
            repository.deleteBudget(RoomTestData.budgetA),
        )
    }

    @Test
    fun deleteBudget_reportsNotFoundOrAlreadyDeletedForUnknownBudget() = runTest {
        assertEquals(
            DeleteResult.NotFoundOrAlreadyDeleted,
            repository.deleteBudget(RoomTestData.budgetId(0xEE)),
        )
    }

    @Test
    fun deleteBudget_keepsOtherBudgetsAndReleasesTheSpan() = runTest {
        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(
                RoomTestData.budget(
                    id = RoomTestData.budgetA,
                    start = date(10, 1),
                    end = date(10, 5),
                    amount = 40_000,
                ),
                RoomTestData.budget(
                    id = RoomTestData.budgetB,
                    start = date(10, 5),
                    end = date(10, 15),
                    amount = 60_000,
                ),
            ),
        )
        val second = budget(
            id = RoomTestData.budgetB,
            start = date(10, 5),
            end = date(10, 15),
            amount = 60_000,
        )

        assertEquals(DeleteResult.Deleted, repository.deleteBudget(RoomTestData.budgetA))

        assertEquals(listOf(second), budgetsOf())

        val replacement = budget(
            id = RoomTestData.budgetC,
            start = date(10, 1),
            end = date(10, 5),
            amount = 50_000,
        )
        assertEquals(BudgetWriteResult.Success, repository.insertBudget(replacement))
        assertEquals(setOf(second, replacement), budgetsOf().toSet())
    }
}
