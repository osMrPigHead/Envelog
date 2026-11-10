package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.db.row.EnvelopeSummaryRow
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID

class MappersTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val envelopeId = EnvelopeId(UUID(0L, 0xA1))
    private val budgetId = BudgetId(UUID(0L, 0xD1))
    private val expenditureId = ExpenditureId(UUID(0L, 0xC1))
    private val start = LocalDate.of(2026, 10, 1)
    private val end = LocalDate.of(2026, 11, 1)
    private val happenedAt = Instant.parse("2026-10-20T04:00:00Z")
    private val expenditure = ExpenditureSubmission(
        id = expenditureId,
        happenedAt = happenedAt,
        happenedZone = zone,
        allocations = listOf(
            ExpenditureAllocation(expenditureId, envelopeId, 12_345),
        ),
    )

    @Test
    fun budgetEntityToDomain_copiesAllFields() {
        val deletedAt = Instant.parse("2026-10-19T04:00:00Z")
        val entity = BudgetEntity(
            id = budgetId,
            envelopeId = envelopeId,
            dateSpan = HalfOpenDateSpan(start, end),
            attributedMonth = YearMonth.of(2026, 10),
            amount = 123_456,
            deletedAt = deletedAt,
        )

        assertEquals(
            Budget(
                id = budgetId,
                envelopeId = envelopeId,
                dateSpan = HalfOpenDateSpan(start, end),
                attributedMonth = YearMonth.of(2026, 10),
                amount = 123_456,
            ),
            entity.toDomain(),
        )
    }

    @Test
    fun envelopeEntityToDomain_mapsArchiveState() {
        val active = EnvelopeEntity(envelopeId, "餐饮", isBudgeted = true, archivedAt = null)
        val archived = active.copy(archivedAt = happenedAt)

        assertEquals(Envelope(envelopeId, "餐饮", true, archived = false), active.toDomain())
        assertEquals(Envelope(envelopeId, "餐饮", true, archived = true), archived.toDomain())
    }

    @Test
    fun envelopeToEntity_mapsActiveEnvelopeWithoutArchiveTime() {
        val envelope = Envelope(envelopeId, "餐饮", isBudgeted = true, archived = false)

        assertEquals(
            EnvelopeEntity(envelopeId, "餐饮", isBudgeted = true, archivedAt = null),
            envelope.toEntity(),
        )
    }

    @Test
    fun envelopeToEntity_rejectsArchivedEnvelopeWithoutArchiveTime() {
        val envelope = Envelope(envelopeId, "旧信封", isBudgeted = true, archived = true)

        assertThrows(IllegalArgumentException::class.java) { envelope.toEntity() }
    }

    @Test
    fun expenditureSubmissionToEntity_copiesEntryFields() {
        val entity = expenditure.toEntity()

        assertEquals(expenditureId, entity.id)
        assertEquals(expenditure.happenedAt, entity.happenedAt)
        assertEquals(expenditure.happenedDate, entity.happenedDate)
        assertEquals(zone, entity.happenedZone)
    }

    @Test
    fun expenditureSubmissionToEntity_startsNotDeleted() {
        assertNull(expenditure.toEntity().deletedAt)
    }

    @Test
    fun expenditureAllocationToEntity_copiesReferencesAndAmount() {
        val entity = expenditure.allocations.single().toEntity()

        assertEquals(expenditureId, entity.expenditureId)
        assertEquals(envelopeId, entity.envelopeId)
        assertEquals(12_345L, entity.amount)
    }

    @Test
    fun expenditureAllocationToEntity_keepsAmountIndependentOfOtherAllocations() {
        val other = ExpenditureAllocation(expenditureId, EnvelopeId(UUID(0L, 3L)), 1)

        assertEquals(12_345L, expenditure.allocations.single().toEntity().amount)
        assertEquals(1L, other.toEntity().amount)
    }

    @Test
    fun summaryRowToDomain_mapsBudgetedRow() {
        val row = summaryRow(
            archived = true,
            allocated = 39_000,
            totalBudget = 180_000,
            currentBudget = 60_000,
            currentSpanStartDate = LocalDate.of(2026, 10, 15),
            currentSpanEndDate = LocalDate.of(2026, 10, 22),
            currentSpanAllocated = 20_000,
            priorBudget = 40_000,
            priorAllocated = 12_000,
        )

        assertEquals(
            EnvelopeSummary.Budgeted(
                id = envelopeId,
                name = "餐饮",
                allocated = 39_000,
                currency = CurrencyType.CNY,
                archived = true,
                totalBudget = 180_000,
                currentBudget = 60_000,
                currentSpan = HalfOpenDateSpan(
                    LocalDate.of(2026, 10, 15),
                    LocalDate.of(2026, 10, 22),
                ),
                currentSpanAllocated = 20_000,
                priorBudget = 40_000,
                priorAllocated = 12_000,
            ),
            row.toDomain(),
        )
    }

    @Test
    fun summaryRowToDomain_usesZeroForMissingBudgetAggregates() {
        val row = summaryRow(
            allocated = 5_000,
            totalBudget = null,
            currentBudget = null,
            currentSpanStartDate = null,
            currentSpanEndDate = null,
            currentSpanAllocated = null,
            priorBudget = null,
            priorAllocated = null,
        )

        val summary = row.toDomain() as EnvelopeSummary.Budgeted

        assertEquals(0L, summary.totalBudget)
        assertNull(summary.currentBudget)
        assertNull(summary.currentSpan)
        assertEquals(0L, summary.currentSpanAllocated)
        assertEquals(0L, summary.priorBudget)
        assertEquals(0L, summary.priorAllocated)
    }

    @Test
    fun summaryRowToDomain_keepsCurrentSpanNullWhenOnlyOneBoundaryExists() {
        val onlyStart = summaryRow(
            currentSpanStartDate = LocalDate.of(2026, 10, 15),
            currentSpanEndDate = null,
        )
        val onlyEnd = summaryRow(
            currentSpanStartDate = null,
            currentSpanEndDate = LocalDate.of(2026, 10, 22),
        )

        assertNull((onlyStart.toDomain() as EnvelopeSummary.Budgeted).currentSpan)
        assertNull((onlyEnd.toDomain() as EnvelopeSummary.Budgeted).currentSpan)
    }

    @Test
    fun summaryRowToDomain_mapsNonBudgetRowAndIgnoresBudgetColumns() {
        val row = summaryRow(
            isBudgeted = false,
            archived = true,
            allocated = 30_000,
            totalBudget = 100_000,
            currentBudget = 40_000,
        )

        assertEquals(
            EnvelopeSummary.NonBudget(envelopeId, "餐饮", 30_000, CurrencyType.CNY, archived = true),
            row.toDomain(),
        )
    }

    @Test
    fun allocationRowToDomain_mapsExpenditureInfo() {
        val row = AllocationWithInfoRow(
            expenditureId = expenditureId,
            happenedAt = happenedAt,
            happenedDate = LocalDate.of(2026, 10, 20),
            happenedZone = zone,
            envelopeId = envelopeId,
            amount = 6_000,
        )

        assertEquals(
            AllocationWithInfo(
                expenditure = ExpenditureMetaInfo(
                    id = expenditureId,
                    happenedAt = happenedAt,
                    happenedDate = LocalDate.of(2026, 10, 20),
                    happenedZone = zone,
                ),
                envelopeId = envelopeId,
                amount = 6_000,
            ),
            row.toDomain(),
        )
    }

    private fun summaryRow(
        isBudgeted: Boolean = true,
        archived: Boolean = false,
        allocated: Long = 0,
        totalBudget: Long? = null,
        currentBudget: Long? = null,
        currentSpanStartDate: LocalDate? = null,
        currentSpanEndDate: LocalDate? = null,
        currentSpanAllocated: Long? = null,
        priorBudget: Long? = null,
        priorAllocated: Long? = null,
    ): EnvelopeSummaryRow = EnvelopeSummaryRow(
        id = envelopeId,
        name = "餐饮",
        isBudgeted = isBudgeted,
        archived = archived,
        allocated = allocated,
        totalBudget = totalBudget,
        currentBudget = currentBudget,
        currentSpanStartDate = currentSpanStartDate,
        currentSpanEndDate = currentSpanEndDate,
        currentSpanAllocated = currentSpanAllocated,
        priorBudget = priorBudget,
        priorAllocated = priorAllocated,
    )
}
