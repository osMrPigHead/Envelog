package cn.osmrpighead.envelog.data.repository

import android.app.Application
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.repository.room.RoomEnvelogRepository
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
import java.time.Clock
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomEnvelogRepositoryQueryTest {
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

    @Test
    fun observeAllocations_mapsExpenditureInfoAndFiltersEnvelopeAndSpan() = runTest {
        val (inside, insideShares) = RoomTestData.record(
            0xB1,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 6_000,
            RoomTestData.envelopeB to 4_000,
        )
        val (outsideSpan, outsideSpanShares) = RoomTestData.record(
            0xB2,
            LocalDate.of(2026, 11, 1),
            RoomTestData.envelopeA to 7_000,
        )

        database.seed(
            envelopes = listOf(
                RoomTestData.envelope(RoomTestData.envelopeA),
                RoomTestData.envelope(RoomTestData.envelopeB, name = "购物", isBudgeted = false),
            ),
            expenditures = listOf(inside, outsideSpan),
            allocations = insideShares + outsideSpanShares,
        )

        assertEquals(
            listOf(
                AllocationWithInfo(
                    expenditure = ExpenditureMetaInfo(
                        id = RoomTestData.expenditureId(0xB1),
                        happenedAt = RoomTestData.at(LocalDate.of(2026, 10, 5)),
                        happenedDate = LocalDate.of(2026, 10, 5),
                        happenedZone = RoomTestData.zone,
                    ),
                    envelopeId = RoomTestData.envelopeA,
                    amount = 6_000,
                ),
            ),
            repository.observeAllocations(
                RoomTestData.envelopeA,
                RoomTestData.month.toHalfOpenDateSpan(),
            ).first(),
        )
    }

    @Test
    fun observeBudgets_mapsBudgetEntitiesByAttributedMonth() = runTest {
        val current = RoomTestData.budget(
            id = RoomTestData.budgetA,
            start = RoomTestData.monthStart,
            end = RoomTestData.monthEnd,
            attributedMonth = RoomTestData.month,
            amount = 100_000,
        )
        val nextMonth = RoomTestData.budget(
            id = RoomTestData.budgetB,
            start = RoomTestData.monthEnd,
            end = RoomTestData.monthEnd.plusMonths(1),
            attributedMonth = RoomTestData.month.plusMonths(1),
            amount = 80_000,
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            budgets = listOf(current, nextMonth),
        )

        assertEquals(
            listOf(
                Budget(
                    id = RoomTestData.budgetA,
                    envelopeId = RoomTestData.envelopeA,
                    dateSpan = HalfOpenDateSpan(
                        RoomTestData.monthStart,
                        RoomTestData.monthEnd,
                    ),
                    attributedMonth = RoomTestData.month,
                    amount = 100_000,
                ),
            ),
            repository.observeBudgets(RoomTestData.envelopeA, RoomTestData.month).first(),
        )
    }

    @Test
    fun observeDailyAllocationOverview_mapsGroupedRows() = runTest {
        val (first, firstShares) = RoomTestData.record(
            0xB1,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 6_000,
        )
        val (second, secondShares) = RoomTestData.record(
            0xB2,
            LocalDate.of(2026, 10, 5),
            RoomTestData.envelopeA to 1_000,
        )
        val (lastDay, lastDayShares) = RoomTestData.record(
            0xB3,
            LocalDate.of(2026, 10, 31),
            RoomTestData.envelopeA to 5_000,
        )
        val (nextMonth, nextMonthShares) = RoomTestData.record(
            0xB4,
            LocalDate.of(2026, 11, 1),
            RoomTestData.envelopeA to 7_000,
        )

        database.seed(
            envelopes = listOf(RoomTestData.envelope()),
            expenditures = listOf(first, second, lastDay, nextMonth),
            allocations = firstShares + secondShares + lastDayShares + nextMonthShares,
        )

        assertEquals(
            setOf(
                DailyAllocationSummary(RoomTestData.envelopeA, LocalDate.of(2026, 10, 5), 7_000),
                DailyAllocationSummary(RoomTestData.envelopeA, LocalDate.of(2026, 10, 31), 5_000),
            ),
            repository.observeDailyAllocationOverview(
                RoomTestData.envelopeA,
                RoomTestData.month.toHalfOpenDateSpan(),
            ).first().toSet(),
        )
    }
}
