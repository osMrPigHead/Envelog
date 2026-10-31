package cn.osmrpighead.envelog.data.repository

import android.app.Application
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.testing.RoomTestData
import cn.osmrpighead.envelog.testing.seed
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomEnvelogRepositoryTest {
    private lateinit var database: EnvelogDatabase
    private lateinit var repository: RoomEnvelogRepository

    private val expenditureId = ExpenditureId(RoomTestData.uuid(0xC1))

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

    private suspend fun overview(): List<EnvelopeSummary> = repository.observeOverview(
        today = RoomTestData.today,
        yearMonth = RoomTestData.month,
        dateSpan = RoomTestData.month.toHalfOpenDateSpan(),
    ).first()

    private fun newExpenditure(
        allocations: List<ExpenditureAllocation>,
        date: LocalDate = RoomTestData.monthStart.plusDays(4),
        id: ExpenditureId = expenditureId,
        happenedAt: Instant = RoomTestData.at(date),
        happenedDate: LocalDate = date,
    ): Expenditure = Expenditure(
        id = id,
        happenedAt = happenedAt,
        happenedDate = happenedDate,
        happenedZone = RoomTestData.zone,
        allocations = allocations,
    )

    private fun allocation(envelopeId: EnvelopeId, amount: Long): ExpenditureAllocation =
        ExpenditureAllocation(expenditureId, envelopeId, amount)

    private suspend fun assertRejected(block: suspend () -> Unit) {
        try {
            block()
        } catch (expected: IllegalArgumentException) {
            return
        }
        fail("Expected IllegalArgumentException")
    }

    private suspend fun assertNothingRecorded() {
        assertTrue("预期没有写入任何分摊，实际为 ${overview()}", overview().all { it.allocated == 0L })
    }

    private suspend fun seedSplitBudgets() {
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
    }

    @Test
    fun observeOverview_mapsBudgetedSummary() = runTest {
        seedSplitBudgets()

        val summary = overview().single()

        assertEquals(
            EnvelopeSummary.Budgeted(
                id = RoomTestData.envelopeA,
                name = "信封",
                allocated = 39_000,
                currency = CurrencyType.CNY,
                totalBudget = 180_000,
                currentBudget = 60_000,
                currentSpan = LocalDate.of(2026, 10, 15) untilExclusive LocalDate.of(2026, 10, 22),
                currentSpanAllocated = 20_000,
                priorBudget = 40_000,
                priorAllocated = 12_000,
            ),
            summary,
        )
    }

    @Test
    fun observeOverview_derivesBudgetedAmounts() = runTest {
        seedSplitBudgets()

        val summary = overview().single() as EnvelopeSummary.Budgeted

        assertEquals(40_000L, summary.currentAvailable)
        assertEquals(28_000L, summary.priorDifference)
        assertTrue(summary.usedProportion.isValid)
        assertEquals(0, BigDecimal("0.2166667").compareTo(summary.usedProportion.ratio))
    }

    @Test
    fun observeOverview_mapsNonBudgetedSummary() = runTest {
        val (expenditure, shares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 12), RoomTestData.envelopeA to 30_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope(name = "购物", isBudgeted = false)),
            expenditures = listOf(expenditure),
            allocations = shares,
        )

        assertEquals(
            listOf(EnvelopeSummary.NonBudget(RoomTestData.envelopeA, "购物", 30_000, CurrencyType.CNY)),
            overview(),
        )
    }

    @Test
    fun observeOverview_mapsBudgetedEnvelopeWithoutBudgetRows() = runTest {
        val (expenditure, shares) = RoomTestData.record(0xB1, LocalDate.of(2026, 10, 3), RoomTestData.envelopeA to 5_000)

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            expenditures = listOf(expenditure),
            allocations = shares,
        )

        val summary = overview().single() as EnvelopeSummary.Budgeted

        assertEquals(5_000L, summary.allocated)
        assertEquals(0L, summary.totalBudget)
        assertNull(summary.currentBudget)
        assertNull(summary.currentSpan)
        assertEquals(0L, summary.currentSpanAllocated)
        assertEquals(0L, summary.priorBudget)
        // 没有当前预算时，历史窗口的终点回退到 today，因此月初至今的支出都算历史支出
        assertEquals(5_000L, summary.priorAllocated)
        assertFalse(summary.usedProportion.isValid)
    }

    @Test
    fun archiveEnvelope_archivesOnceThenReportsNotFound() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertEquals(ArchiveResult.Archived, repository.archiveEnvelope(RoomTestData.envelopeA))
        assertNotNull(database.envelopeDao().getById(RoomTestData.envelopeA)?.archivedAt)
        assertEquals(ArchiveResult.NotFound, repository.archiveEnvelope(RoomTestData.envelopeA))
    }

    @Test
    fun archiveEnvelope_reportsNotFoundForUnknownEnvelope() = runTest {
        assertEquals(ArchiveResult.NotFound, repository.archiveEnvelope(RoomTestData.envelopeA))
    }

    @Test
    fun recordExpenditure_writesExpenditureAndAllocations() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope(RoomTestData.envelopeA)))

        repository.recordExpenditure(
            newExpenditure(allocations = listOf(allocation(RoomTestData.envelopeA, 6_000))),
        )

        assertEquals(6_000L, overview().single().allocated)
    }

    @Test
    fun recordExpenditure_rejectsEmptyAllocations() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertRejected { repository.recordExpenditure(newExpenditure(allocations = emptyList())) }
        assertNothingRecorded()
    }

    @Test
    fun recordExpenditure_rejectsHappenedDateMismatch() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertRejected {
            repository.recordExpenditure(
                newExpenditure(
                    allocations = listOf(allocation(RoomTestData.envelopeA, 10_000)),
                    date = RoomTestData.monthStart.plusDays(4),
                    happenedDate = RoomTestData.monthStart.plusDays(5),
                ),
            )
        }
        assertNothingRecorded()
    }

    @Test
    fun recordExpenditure_rejectsDuplicateEnvelopes() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertRejected {
            repository.recordExpenditure(
                newExpenditure(
                    allocations = listOf(
                        allocation(RoomTestData.envelopeA, 5_000),
                        allocation(RoomTestData.envelopeA, 5_000),
                    ),
                ),
            )
        }
        assertNothingRecorded()
    }

    @Test
    fun recordExpenditure_rejectsNonPositiveAmount() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertRejected {
            repository.recordExpenditure(
                newExpenditure(allocations = listOf(allocation(RoomTestData.envelopeA, 0))),
            )
        }
        assertNothingRecorded()
    }

    @Test
    fun recordExpenditure_rejectsAllocationOfAnotherExpenditure() = runTest {
        database.seed(envelopes = listOf(RoomTestData.envelope()))

        assertRejected {
            repository.recordExpenditure(
                newExpenditure(
                    allocations = listOf(
                        ExpenditureAllocation(
                            expenditureId = ExpenditureId(RoomTestData.uuid(0xC2)),
                            envelopeId = RoomTestData.envelopeA,
                            amount = 10_000,
                        ),
                    ),
                ),
            )
        }
        assertNothingRecorded()
    }

    @Test
    fun recordExpenditure_rejectsArchivedEnvelope() = runTest {
        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(archivedAt = Instant.parse("2026-10-01T04:00:00Z")),
            ),
        )

        assertRejected {
            repository.recordExpenditure(
                newExpenditure(allocations = listOf(allocation(RoomTestData.envelopeA, 10_000))),
            )
        }
        assertNothingRecorded()
    }
}
