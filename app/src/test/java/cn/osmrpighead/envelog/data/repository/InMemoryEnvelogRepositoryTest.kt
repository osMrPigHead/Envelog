package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.db.entity.EntryType
import cn.osmrpighead.envelog.data.query_old.DateRangeRule
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.ArgumentException.EnvelopeReferenced
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.ArgumentException.NameBlank
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.ArgumentException.NoSuch
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.RuleViolation.AllocationMismatch
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException.RuleViolation.BudgetOverlap
import cn.osmrpighead.envelog.core.time.untilExclusive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class InMemoryEnvelogRepositoryTest {
    // region Envelope

    @Test
    fun observeActiveEnvelopes_excludesArchived() = runTest {
        val repository = TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId),
                TestData.envelope(id = TestData.archivedEnvelopeId, archived = true),
            ),
        )

        val result = repository.observeActiveEnvelopes().first()

        assertEquals(listOf(TestData.envelopeAId), result.map { it.id })
    }

    @Test
    fun createEnvelope_trimsNameAndAddsEnvelope() = runTest {
        val repository = TestData.repository()

        val id = repository.createEnvelope("  Food  ", isBudgeted = true).getOrThrow()

        val envelope = repository.observeActiveEnvelopes().first().single()
        assertEquals(id, envelope.id)
        assertEquals("Food", envelope.name)
        assertTrue(envelope.isBudgeted)
        assertFalse(envelope.archivedAt)
    }

    @Test
    fun createEnvelope_rejectsBlankName() = runTest {
        val repository = TestData.repository()

        repository.createEnvelope("   ", isBudgeted = true).assertFailureIs(NameBlank::class.java)

        assertTrue(repository.observeActiveEnvelopes().first().isEmpty())
    }

    @Test
    fun renameEnvelope_updatesTrimmedName() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope(name = "Old")))

        repository.renameEnvelope(TestData.envelopeAId, "  New  ").getOrThrow()

        assertEquals("New", repository.observeActiveEnvelopes().first().single().name)
    }

    @Test
    fun renameEnvelope_rejectsBlankName() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope(name = "Old")))

        repository.renameEnvelope(TestData.envelopeAId, "  ").assertFailureIs(NameBlank::class.java)

        assertEquals("Old", repository.observeActiveEnvelopes().first().single().name)
    }

    @Test
    fun renameEnvelope_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        repository.renameEnvelope(TestData.unknownId, "New").assertFailureIs(NoSuch.Envelope::class.java)
    }

    @Test
    fun archiveEnvelope_removesEnvelopeFromActiveList() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope()))

        repository.archiveEnvelope(TestData.envelopeAId).getOrThrow()

        assertTrue(repository.observeActiveEnvelopes().first().isEmpty())
    }

    @Test
    fun archiveEnvelope_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        repository.archiveEnvelope(TestData.unknownId).assertFailureIs(NoSuch.Envelope::class.java)
    }

    @Test
    fun deleteEnvelope_removesEnvelopeWithoutAllocations() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope()))

        repository.deleteEnvelope(TestData.envelopeAId).getOrThrow()

        assertTrue(repository.observeActiveEnvelopes().first().isEmpty())
    }

    @Test
    fun deleteEnvelope_rejectsReferencedEnvelope() = runTest {
        val transaction = TestData.transaction()
        val repository = TestData.repository(
            envelopes = listOf(TestData.envelope()),
            transactions = listOf(transaction),
            allocations = listOf(TestData.allocation(transactionId = transaction.id)),
        )

        repository.deleteEnvelope(TestData.envelopeAId).assertFailureIs(EnvelopeReferenced::class.java)

        assertEquals(1, repository.observeActiveEnvelopes().first().size)
    }

    @Test
    fun deleteEnvelope_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        repository.deleteEnvelope(TestData.unknownId).assertFailureIs(NoSuch.Envelope::class.java)
    }

    @Test
    fun getEnvelope_returnsEnvelopeById() = runTest {
        val requested = TestData.envelope(id = TestData.envelopeBId, name = "Gift", isBudgeted = false)
        val repository = TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId),
                requested,
            ),
        )

        assertEquals(requested, repository.getEnvelope(TestData.envelopeBId))
    }

    @Test
    fun getEnvelope_returnsArchivedEnvelope() = runTest {
        val archived = TestData.envelope(id = TestData.archivedEnvelopeId, archived = true)
        val repository = TestData.repository(envelopes = listOf(archived))

        assertEquals(archived, repository.getEnvelope(TestData.archivedEnvelopeId))
    }

    @Test
    fun getEnvelope_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        runCatching { repository.getEnvelope(TestData.unknownId) }
            .assertFailureIs(NoSuch.Envelope::class.java)
    }

    @Test
    fun observeActiveEnvelopesWithExtra_joinsBudgetsAndAllocations() = runTest {
        val budget = TestData.budget()
        val otherBudget = TestData.budget(id = TestData.budgetBId, envelopeId = TestData.envelopeBId)
        val transaction = TestData.transaction()
        val repository = TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId, isBudgeted = true),
                TestData.envelope(id = TestData.envelopeBId, name = "Gift", isBudgeted = false),
                TestData.envelope(id = TestData.archivedEnvelopeId, archived = true),
            ),
            budgets = listOf(budget, otherBudget),
            transactions = listOf(transaction),
            allocations = listOf(TestData.allocation(transactionId = transaction.id)),
        )

        val results = repository
            .observeActiveEnvelopesWithExtra(TestData.period)
            .first()

        assertEquals(listOf(TestData.envelopeAId, TestData.envelopeBId), results.map { it.envelope.id })
        assertEquals(listOf(budget), results[0].budgets)
        assertTrue(results[1].budgets.isEmpty())
        assertEquals(listOf(transaction.id), results[0].allocations.map { it.transaction.id })
        assertTrue(results[1].allocations.isEmpty())
    }

    @Test
    fun observeEnvelopeWithExtra_joinsFilteredBudgetsAndAllocations() = runTest {
        val envelope = TestData.envelope(id = TestData.envelopeAId)
        val inPeriodBudget = TestData.budget(id = TestData.budgetAId)
        val outOfPeriodBudget = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodEnd,
            endDate = TestData.periodEnd.plusDays(10),
        )
        val otherEnvelopeBudget = TestData.budget(id = TestData.budgetCId, envelopeId = TestData.envelopeBId)
        val inPeriodTransaction = TestData.transaction(id = TestData.transactionAId, amount = 6_000)
        val outOfPeriodTransaction = TestData.transaction(id = TestData.transactionBId, date = TestData.periodEnd)
        val repository = TestData.repository(
            envelopes = listOf(envelope, TestData.envelope(id = TestData.envelopeBId, isBudgeted = false)),
            budgets = listOf(inPeriodBudget, outOfPeriodBudget, otherEnvelopeBudget),
            transactions = listOf(inPeriodTransaction, outOfPeriodTransaction),
            allocations = listOf(
                TestData.allocation(
                    transactionId = inPeriodTransaction.id,
                    envelopeId = TestData.envelopeAId,
                    amount = 6_000,
                ),
                TestData.allocation(
                    transactionId = inPeriodTransaction.id,
                    envelopeId = TestData.envelopeBId,
                    amount = 4_000,
                ),
                TestData.allocation(
                    transactionId = outOfPeriodTransaction.id,
                    envelopeId = TestData.envelopeAId,
                    amount = 1_000,
                ),
            ),
        )

        val result = repository
            .observeEnvelopeWithExtra(TestData.envelopeAId, TestData.period)
            .first()

        assertEquals(envelope, result.envelope)
        assertEquals(listOf(inPeriodBudget), result.budgets)
        assertEquals(listOf(inPeriodTransaction.id), result.allocations.map { it.transaction.id })
        assertEquals(6_000L, result.allocations.single().allocation.amount)
    }

    @Test
    fun observeEnvelopeWithExtra_returnsNoBudgetsWhenNotBudgeted() = runTest {
        val envelope = TestData.envelope(id = TestData.envelopeBId, isBudgeted = false)
        val transaction = TestData.transaction()
        val repository = TestData.repository(
            envelopes = listOf(envelope),
            budgets = listOf(TestData.budget(id = TestData.budgetAId, envelopeId = TestData.envelopeBId)),
            transactions = listOf(transaction),
            allocations = listOf(
                TestData.allocation(transactionId = transaction.id, envelopeId = TestData.envelopeBId),
            ),
        )

        val result = repository
            .observeEnvelopeWithExtra(TestData.envelopeBId, TestData.period)
            .first()

        assertEquals(envelope, result.envelope)
        assertTrue(result.budgets.isEmpty())
        assertEquals(listOf(transaction.id), result.allocations.map { it.transaction.id })
    }

    @Test
    fun observeEnvelopeWithExtra_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        runCatching { repository.observeEnvelopeWithExtra(TestData.unknownId, TestData.period) }
            .assertFailureIs(NoSuch.Envelope::class.java)
    }

    // endregion

    // region Budget

    @Test
    fun observeBudgets_filtersByEnvelopeAndStartInRange() = runTest {
        val inPeriod = TestData.budget(id = TestData.budgetAId)
        val startsBefore = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart.minusDays(5),
            endDate = TestData.periodStart.plusDays(5),
        )
        val startsAtEnd = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodEnd,
            endDate = TestData.periodEnd.plusDays(10),
        )
        val otherEnvelope = TestData.budget(
            id = TestData.budgetDId,
            envelopeId = TestData.envelopeBId,
        )
        val repository = TestData.repository(budgets = listOf(inPeriod, startsBefore, startsAtEnd, otherEnvelope))

        val result = repository
            .observeBudgets(TestData.envelopeAId, TestData.period)
            .first()

        assertEquals(listOf(inPeriod), result)
    }

    @Test
    fun observeBudgets_filtersByEndInRangeRule() = runTest {
        val endsInPeriod = TestData.budget(
            id = TestData.budgetAId,
            startDate = TestData.periodStart.minusDays(6),
            endDate = TestData.periodStart.plusDays(9),
        )
        val endsAtPeriodEnd = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart.plusDays(9),
            endDate = TestData.periodEnd,
        )
        val endsBeforeStart = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodStart.minusDays(20),
            endDate = TestData.periodStart.minusDays(1),
        )
        val endsAfterEnd = TestData.budget(
            id = TestData.budgetDId,
            startDate = TestData.periodStart.plusDays(9),
            endDate = TestData.periodEnd.plusDays(5),
        )
        val repository = TestData.repository(
            budgets = listOf(endsInPeriod, endsAtPeriodEnd, endsBeforeStart, endsAfterEnd),
        )

        val result = repository
            .observeBudgets(TestData.envelopeAId, TestData.range(rule = DateRangeRule.EndInRange))
            .first()

        assertEquals(listOf(endsInPeriod), result)
    }

    @Test
    fun observeBudgets_filtersByMajorityOverlapFallbackStart() = runTest {
        // 共 8 天，重叠 5 天：多数重叠，不受回退影响
        val majorityOverlap = TestData.budget(
            id = TestData.budgetAId,
            startDate = TestData.periodStart.minusDays(3),
            endDate = TestData.periodStart.plusDays(5),
        )
        // 共 24 天，重叠 12 天：恰好一半，回退到 StartInRange，起点在区间内
        val tieStartInside = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart.plusDays(19),
            endDate = TestData.periodEnd.plusDays(12),
        )
        // 共 14 天，重叠 7 天：恰好一半，回退到 StartInRange，起点在区间外
        val tieStartOutside = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodStart.minusDays(7),
            endDate = TestData.periodStart.plusDays(7),
        )
        // 共 76 天，重叠 31 天：不到一半，即使整体包住查询区间也不算命中
        val containing = TestData.budget(
            id = TestData.budgetDId,
            startDate = TestData.periodStart.minusDays(30),
            endDate = TestData.periodEnd.plusDays(15),
        )
        val repository = TestData.repository(
            budgets = listOf(majorityOverlap, tieStartInside, tieStartOutside, containing),
        )

        val result = repository
            .observeBudgets(TestData.envelopeAId, TestData.range(rule = DateRangeRule.MajorityOverlapFallbackStart))
            .first()

        assertEquals(listOf(majorityOverlap, tieStartInside), result)
    }

    @Test
    fun observeBudgets_filtersByMajorityOverlapFallbackEnd() = runTest {
        // 共 8 天，重叠 5 天：多数重叠，不受回退影响
        val majorityOverlap = TestData.budget(
            id = TestData.budgetAId,
            startDate = TestData.periodStart.minusDays(3),
            endDate = TestData.periodStart.plusDays(5),
        )
        // 共 24 天，重叠 12 天：恰好一半，回退到 EndInRange，终点在区间外
        val tieEndOutside = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart.plusDays(19),
            endDate = TestData.periodEnd.plusDays(12),
        )
        // 共 14 天，重叠 7 天：恰好一半，回退到 EndInRange，终点在区间内
        val tieEndInside = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodStart.minusDays(7),
            endDate = TestData.periodStart.plusDays(7),
        )
        // 共 76 天，重叠 31 天：不到一半，即使整体包住查询区间也不算命中
        val containing = TestData.budget(
            id = TestData.budgetDId,
            startDate = TestData.periodStart.minusDays(30),
            endDate = TestData.periodEnd.plusDays(15),
        )
        val repository = TestData.repository(
            budgets = listOf(majorityOverlap, tieEndOutside, tieEndInside, containing),
        )

        val result = repository
            .observeBudgets(TestData.envelopeAId, TestData.range(rule = DateRangeRule.MajorityOverlapFallbackEnd))
            .first()

        assertEquals(listOf(majorityOverlap, tieEndInside), result)
    }

    @Test
    fun observeBudgets_filtersByOverlapRule() = runTest {
        val inside = TestData.budget(
            id = TestData.budgetAId,
            startDate = TestData.periodStart.plusDays(4),
            endDate = TestData.periodStart.plusDays(9),
        )
        val containing = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart.minusDays(30),
            endDate = TestData.periodEnd.plusDays(15),
        )
        val touchingStart = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodStart.minusDays(5),
            endDate = TestData.periodStart,
        )
        val touchingEnd = TestData.budget(
            id = TestData.budgetDId,
            startDate = TestData.periodEnd,
            endDate = TestData.periodEnd.plusDays(5),
        )
        val repository = TestData.repository(
            budgets = listOf(inside, containing, touchingStart, touchingEnd),
        )

        val result = repository
            .observeBudgets(TestData.envelopeAId, TestData.overlapPeriod)
            .first()

        assertEquals(listOf(inside, containing), result)
    }

    @Test
    fun setBudget_createsBudget() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope()))

        val id = repository.setBudget(
            envelopeId = TestData.envelopeAId,
            period = TestData.periodStart untilExclusive TestData.periodStart.plusDays(10),
            amount = 50_000,
        ).getOrThrow()

        val budget = repository
            .observeBudgets(TestData.envelopeAId, TestData.period)
            .first()
            .single()
        assertEquals(id, budget.id)
        assertEquals(50_000L, budget.amount)
        assertEquals(TestData.periodStart, budget.startDate)
        assertEquals(TestData.periodStart.plusDays(10), budget.endDate)
    }

    @Test
    fun setBudget_allowsAdjacentPeriodsAndRejectsOverlaps() = runTest {
        val existing = TestData.budget(
            startDate = TestData.periodStart,
            endDate = TestData.periodStart.plusDays(10),
        )
        val repository = TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId),
                TestData.envelope(id = TestData.envelopeBId),
            ),
            budgets = listOf(existing),
        )

        assertTrue(
            repository.setBudget(
                TestData.envelopeAId,
                TestData.periodStart.plusDays(10) untilExclusive TestData.periodStart.plusDays(20),
                10_000,
            ).isSuccess,
        )
        repository.setBudget(
            TestData.envelopeAId,
            TestData.periodStart.plusDays(9) untilExclusive TestData.periodStart.plusDays(11),
            10_000,
        ).assertFailureIs(BudgetOverlap::class.java)
        repository.setBudget(
            TestData.envelopeAId,
            TestData.periodStart untilExclusive TestData.periodStart.plusDays(10),
            10_000,
        ).assertFailureIs(BudgetOverlap::class.java)
        repository.setBudget(
            TestData.envelopeAId,
            TestData.periodStart.minusDays(5) untilExclusive TestData.periodStart.plusDays(5),
            10_000,
        ).assertFailureIs(BudgetOverlap::class.java)
        assertTrue(
            repository.setBudget(
                TestData.envelopeBId,
                TestData.periodStart untilExclusive TestData.periodStart.plusDays(10),
                10_000,
            ).isSuccess,
        )
    }

    /*
    TODO: “未写入”断言用 observeBudgets(unknownId, ...) 验证，而该查询本身按信封过滤，结果恒为空，无法证明没写入；
     若要真正固定这一行为，需要仓库提供不带信封过滤的预算查询，或改为断言其他信封下也没有意外数据。
     */
    @Test
    fun setBudget_rejectsUnknownEnvelope() = runTest {
        val repository = TestData.repository()

        repository.setBudget(
            TestData.unknownId,
            TestData.periodStart untilExclusive TestData.periodEnd,
            10_000,
        ).assertFailureIs(NoSuch.Envelope::class.java)

        assertTrue(
            repository.observeBudgets(TestData.unknownId, TestData.period)
                .first()
                .isEmpty(),
        )
    }

    @Test
    fun setBudget_updatesAmountById() = runTest {
        val budget = TestData.budget(amount = 10_000)
        val repository = TestData.repository(budgets = listOf(budget))

        repository.setBudget(budget.id, 20_000).getOrThrow()

        val updated = repository
            .observeBudgets(TestData.envelopeAId, TestData.period)
            .first()
            .single()
        assertEquals(20_000L, updated.amount)
    }

    @Test
    fun setBudget_rejectsUnknownBudgetId() = runTest {
        val repository = TestData.repository()

        repository.setBudget(TestData.unknownId, 20_000).assertFailureIs(NoSuch.Budget::class.java)
    }

    @Test
    fun setBudget_rejectsPeriodContainingExistingBudget() = runTest {
        val existing = TestData.budget(
            startDate = TestData.periodStart.plusDays(4),
            endDate = TestData.periodStart.plusDays(9),
        )
        val repository = TestData.repository(
            envelopes = listOf(TestData.envelope()),
            budgets = listOf(existing),
        )

        repository.setBudget(
            TestData.envelopeAId,
            TestData.periodStart untilExclusive TestData.periodEnd,
            10_000,
        ).assertFailureIs(BudgetOverlap::class.java)

        assertEquals(
            listOf(existing),
            repository.observeBudgets(TestData.envelopeAId, TestData.period).first(),
        )
    }

    @Test
    fun setBudget_allowsPeriodTouchingExistingStart() = runTest {
        val existing = TestData.budget(
            startDate = TestData.periodStart,
            endDate = TestData.periodStart.plusDays(10),
        )
        val repository = TestData.repository(
            envelopes = listOf(TestData.envelope()),
            budgets = listOf(existing),
        )

        val id = repository.setBudget(
            TestData.envelopeAId,
            TestData.periodStart.minusDays(10) untilExclusive TestData.periodStart,
            10_000,
        ).getOrThrow()

        assertEquals(
            setOf(existing.id, id),
            repository.observeBudgets(
                TestData.envelopeAId,
                TestData.range(TestData.periodStart.minusDays(30), TestData.periodEnd),
            ).first().map { it.id }.toSet(),
        )
    }

    @Test
    fun clearBudget_removesBudgetById() = runTest {
        val budget = TestData.budget()
        val repository = TestData.repository(budgets = listOf(budget))

        assertEquals(budget, repository.clearBudget(budget.id).getOrThrow())

        assertTrue(
            repository.observeBudgets(TestData.envelopeAId, TestData.period)
                .first()
                .isEmpty(),
        )
    }

    @Test
    fun clearBudget_rejectsUnknownBudgetId() = runTest {
        val repository = TestData.repository()

        repository.clearBudget(TestData.unknownId).assertFailureIs(NoSuch.Budget::class.java)
    }

    @Test
    fun clearBudget_removesOverlappingBudgetsOnly() = runTest {
        val before = TestData.budget(
            id = TestData.budgetAId,
            startDate = TestData.periodStart.minusDays(11),
            endDate = TestData.periodStart,
        )
        val first = TestData.budget(
            id = TestData.budgetBId,
            startDate = TestData.periodStart,
            endDate = TestData.periodStart.plusDays(9),
        )
        val second = TestData.budget(
            id = TestData.budgetCId,
            startDate = TestData.periodStart.plusDays(19),
            endDate = TestData.periodEnd,
        )
        val after = TestData.budget(
            id = TestData.budgetDId,
            startDate = TestData.periodEnd,
            endDate = TestData.periodEnd.plusDays(10),
        )
        val repository = TestData.repository(budgets = listOf(before, first, second, after))

        val removed = repository.clearBudget(TestData.overlapPeriod).getOrThrow()

        assertEquals(setOf(first.id, second.id), removed.map { it.id }.toSet())
        val remaining = repository.observeBudgets(
            TestData.envelopeAId,
            TestData.range(TestData.periodStart.minusDays(30), TestData.periodEnd.plusDays(30)),
        ).first()
        assertEquals(setOf(before.id, after.id), remaining.map { it.id }.toSet())
    }

    @Test
    fun clearBudget_clearsOnlyGivenEnvelope() = runTest {
        val budgetA = TestData.budget(id = TestData.budgetAId, envelopeId = TestData.envelopeAId)
        val budgetB = TestData.budget(id = TestData.budgetBId, envelopeId = TestData.envelopeBId)
        val repository = TestData.repository(budgets = listOf(budgetA, budgetB))

        val removed = repository
            .clearBudget(TestData.envelopeAId, TestData.overlapPeriod)
            .getOrThrow()

        assertEquals(listOf(budgetA), removed)
        assertEquals(
            listOf(budgetB),
            repository.observeBudgets(TestData.envelopeBId, TestData.period).first(),
        )
    }

    @Test
    fun clearBudget_removesBudgetContainingRange() = runTest {
        val containing = TestData.budget(
            startDate = TestData.periodStart.minusDays(30),
            endDate = TestData.periodEnd.plusDays(15),
        )
        val repository = TestData.repository(budgets = listOf(containing))

        assertEquals(listOf(containing), repository.clearBudget(TestData.overlapPeriod).getOrThrow())
    }

    @Test
    fun clearBudget_respectsDateRangeRule() = runTest {
        fun budgets() = listOf(
            // 起点在区间外但与区间重叠：StartInRange 不命中，Overlap 命中
            TestData.budget(
                id = TestData.budgetAId,
                startDate = TestData.periodStart.minusDays(20),
                endDate = TestData.periodStart.plusDays(5),
            ),
            // 起点在区间内：两种规则都命中
            TestData.budget(
                id = TestData.budgetBId,
                startDate = TestData.periodStart.plusDays(9),
                endDate = TestData.periodStart.plusDays(19),
            ),
            // 与区间首尾相接：两种规则都不命中
            TestData.budget(
                id = TestData.budgetCId,
                startDate = TestData.periodEnd,
                endDate = TestData.periodEnd.plusDays(5),
            ),
        )

        val removedByStart = TestData.repository(budgets = budgets())
            .clearBudget(TestData.period)
            .getOrThrow()
        val removedByOverlap = TestData.repository(budgets = budgets())
            .clearBudget(TestData.overlapPeriod)
            .getOrThrow()

        assertEquals(listOf(TestData.budgetBId), removedByStart.map { it.id })
        assertEquals(
            setOf(TestData.budgetAId, TestData.budgetBId),
            removedByOverlap.map { it.id }.toSet(),
        )
    }

    @Test
    fun clearBudget_withUnknownEnvelopeRemovesNothing() = runTest {
        val budget = TestData.budget()
        val repository = TestData.repository(budgets = listOf(budget))

        val removed = repository
            .clearBudget(TestData.unknownId, TestData.overlapPeriod)
            .getOrThrow()

        assertTrue(removed.isEmpty())
        assertEquals(
            listOf(budget),
            repository.observeBudgets(TestData.envelopeAId, TestData.period).first(),
        )
    }

    // endregion

    // region Transaction

    @Test
    fun observeTransactions_filtersByHalfOpenDateRange() = runTest {
        val inside = TestData.transaction(id = TestData.transactionAId, date = TestData.periodStart)
        val onEnd = TestData.transaction(id = TestData.transactionBId, date = TestData.periodEnd)
        val repository = TestData.repository(transactions = listOf(inside, onEnd))

        val result = repository.observeTransactions(TestData.period).first()

        assertEquals(listOf(inside), result)
    }

    @Test
    fun observeTransactions_filtersByType() = runTest {
        val transaction = TestData.transaction()
        val repository = TestData.repository(transactions = listOf(transaction))

        val result = repository
            .observeTransactions(EntryType.Expenditure, TestData.period)
            .first()

        assertEquals(listOf(transaction), result)
    }

    @Test
    fun observeExpenditureEnvelopeAllocations_joinsAndFilters() = runTest {
        val tracked = TestData.transaction(id = TestData.transactionAId, date = TestData.periodStart.plusDays(2))
        val outOfRange = TestData.transaction(id = TestData.transactionBId, date = TestData.periodEnd)
        val repository = TestData.repository(
            transactions = listOf(tracked, outOfRange),
            allocations = listOf(
                TestData.allocation(transactionId = tracked.id, envelopeId = TestData.envelopeAId, amount = 6_000),
                TestData.allocation(transactionId = tracked.id, envelopeId = TestData.envelopeBId, amount = 4_000),
                TestData.allocation(transactionId = outOfRange.id, envelopeId = TestData.envelopeAId, amount = 1_000),
                TestData.allocation(transactionId = TestData.unknownId, envelopeId = TestData.envelopeAId, amount = 2_000),
            ),
        )

        val result = repository.observeExpenditureEnvelopeAllocations(
            TestData.envelopeAId, TestData.period,
        ).first()

        assertEquals(listOf(tracked.id), result.map { it.transaction.id })
        assertEquals(6_000L, result.single().allocation.amount)
    }

    @Test
    fun recordExpenditure_createsTransactionAndAllocations() = runTest {
        val repository = TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId),
                TestData.envelope(id = TestData.envelopeBId),
            ),
        )
        val date = TestData.periodStart.plusDays(3)

        val id = repository.recordExpenditure(
            dateTime = TestData.at(date),
            amount = 10_000,
            allocations = mapOf(TestData.envelopeAId to 6_000, TestData.envelopeBId to 4_000),
        ).getOrThrow()

        val transaction = repository.observeTransactions(TestData.period).first().single()
        assertEquals(id, transaction.id)
        assertEquals(10_000L, transaction.amount)
        assertEquals(date, transaction.happenedAt.toLocalDate())

        val allocationA = repository.observeExpenditureEnvelopeAllocations(
            TestData.envelopeAId, TestData.period,
        ).first().single()
        assertEquals(6_000L, allocationA.allocation.amount)
        val allocationB = repository.observeExpenditureEnvelopeAllocations(
            TestData.envelopeBId, TestData.period,
        ).first().single()
        assertEquals(4_000L, allocationB.allocation.amount)
    }

    @Test
    fun recordExpenditure_rejectsAllocationMismatchWithoutWriting() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope()))

        repository.recordExpenditure(
            dateTime = TestData.at(TestData.periodStart),
            amount = 10_000,
            allocations = mapOf(TestData.envelopeAId to 9_000),
        ).assertFailureIs(AllocationMismatch::class.java)

        assertTrue(repository.observeTransactions(TestData.period).first().isEmpty())
        assertTrue(
            repository.observeExpenditureEnvelopeAllocations(
                TestData.envelopeAId, TestData.period,
            ).first().isEmpty(),
        )
    }

    @Test
    fun recordExpenditure_rejectsUnknownEnvelopeWithoutWriting() = runTest {
        val repository = TestData.repository(envelopes = listOf(TestData.envelope()))

        repository.recordExpenditure(
            dateTime = TestData.at(TestData.periodStart),
            amount = 10_000,
            allocations = mapOf(TestData.unknownId to 10_000),
        ).assertFailureIs(NoSuch.Envelope::class.java)

        assertTrue(repository.observeTransactions(TestData.period).first().isEmpty())
    }

    @Test
    fun reallocate_replacesAllocations() = runTest {
        val repository = repositoryWithExpenditure()

        repository.reallocate(
            transactionId = TestData.transactionAId,
            allocations = mapOf(TestData.envelopeBId to 10_000),
        ).getOrThrow()

        assertTrue(
            repository.observeExpenditureEnvelopeAllocations(
                TestData.envelopeAId, TestData.period,
            ).first().isEmpty(),
        )
        val allocationB = repository.observeExpenditureEnvelopeAllocations(
            TestData.envelopeBId, TestData.period,
        ).first().single()
        assertEquals(10_000L, allocationB.allocation.amount)
        assertEquals(
            10_000L,
            repository.observeTransactions(TestData.period).first().single().amount,
        )
    }

    @Test
    fun reallocate_updatesAmountAndAllocations() = runTest {
        val repository = repositoryWithExpenditure()

        repository.reallocate(
            transactionId = TestData.transactionAId,
            amount = 20_000,
            allocations = mapOf(TestData.envelopeAId to 12_000, TestData.envelopeBId to 8_000),
        ).getOrThrow()

        assertEquals(
            20_000L,
            repository.observeTransactions(TestData.period).first().single().amount,
        )
        assertEquals(
            12_000L,
            repository.observeExpenditureEnvelopeAllocations(
                TestData.envelopeAId, TestData.period,
            ).first().single().allocation.amount,
        )
        assertEquals(
            8_000L,
            repository.observeExpenditureEnvelopeAllocations(
                TestData.envelopeBId, TestData.period,
            ).first().single().allocation.amount,
        )
    }

    @Test
    fun reallocate_rejectsMismatchWithoutPartialUpdate() = runTest {
        val repository = repositoryWithExpenditure()

        repository.reallocate(
            transactionId = TestData.transactionAId,
            amount = 20_000,
            allocations = mapOf(TestData.envelopeAId to 15_000),
        ).assertFailureIs(AllocationMismatch::class.java)

        assertEquals(
            10_000L,
            repository.observeTransactions(TestData.period).first().single().amount,
        )
        val allocation = repository.observeExpenditureEnvelopeAllocations(
            TestData.envelopeAId, TestData.period,
        ).first().single()
        assertEquals(10_000L, allocation.allocation.amount)
    }

    @Test
    fun reallocate_rejectsUnknownTransaction() = runTest {
        val repository = repositoryWithExpenditure()

        repository.reallocate(
            transactionId = TestData.unknownId,
            allocations = mapOf(TestData.envelopeAId to 10_000),
        ).assertFailureIs(NoSuch.Transaction::class.java)
    }

    @Test
    fun deleteTransaction_removesTransactionAndAllocations() = runTest {
        val repository = repositoryWithExpenditure()

        repository.deleteTransaction(TestData.transactionAId).getOrThrow()

        assertTrue(repository.observeTransactions(TestData.period).first().isEmpty())
        assertTrue(
            repository.observeExpenditureEnvelopeAllocations(
                TestData.envelopeAId, TestData.period,
            ).first().isEmpty(),
        )
    }

    @Test
    fun deleteTransaction_rejectsUnknownTransaction() = runTest {
        val repository = repositoryWithExpenditure()

        repository.deleteTransaction(TestData.unknownId).assertFailureIs(NoSuch.Transaction::class.java)
    }

    // endregion

    private fun repositoryWithExpenditure(
        amount: Long = 10_000,
        allocations: Map<UUID, Long> = mapOf(TestData.envelopeAId to amount),
    ): InMemoryEnvelogRepository {
        val transaction = TestData.transaction(amount = amount)
        return TestData.repository(
            envelopes = listOf(
                TestData.envelope(id = TestData.envelopeAId),
                TestData.envelope(id = TestData.envelopeBId),
            ),
            transactions = listOf(transaction),
            allocations = allocations.map { (envelopeId, allocated) ->
                TestData.allocation(
                    transactionId = transaction.id,
                    envelopeId = envelopeId,
                    amount = allocated,
                )
            },
        )
    }
}
