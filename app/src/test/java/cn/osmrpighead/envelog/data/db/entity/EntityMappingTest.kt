package cn.osmrpighead.envelog.data.db.entity

import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class EntityMappingTest {
    private val zone: ZoneId = ZoneId.of("Asia/Shanghai")
    private val expenditureId = ExpenditureId(UUID(0L, 1L))
    private val envelopeId = EnvelopeId(UUID(0L, 2L))

    private val expenditure = Expenditure(
        id = expenditureId,
        happenedAt = Instant.parse("2026-10-20T04:00:00Z"),
        happenedDate = LocalDate.of(2026, 10, 20),
        happenedZone = zone,
        allocations = listOf(
            ExpenditureAllocation(expenditureId, envelopeId, 12_345),
        ),
    )

    @Test
    fun expenditureFromDomain_copiesEntryFields() {
        val entity = ExpenditureEntity.fromDomain(expenditure)

        assertEquals(expenditureId, entity.id)
        assertEquals(expenditure.happenedAt, entity.happenedAt)
        assertEquals(expenditure.happenedDate, entity.happenedDate)
        assertEquals(zone, entity.happenedZone)
    }

    @Test
    fun expenditureFromDomain_startsNotDeleted() {
        assertNull(ExpenditureEntity.fromDomain(expenditure).deletedAt)
    }

    @Test
    fun allocationFromDomain_copiesReferencesAndAmount() {
        val allocation = expenditure.allocations.single()

        val entity = ExpenditureAllocationEntity.fromDomain(allocation)

        assertEquals(expenditureId, entity.expenditureId)
        assertEquals(envelopeId, entity.envelopeId)
        assertEquals(12_345L, entity.amount)
    }

    @Test
    fun allocationFromDomain_keepsAmountIndependentOfOtherAllocations() {
        val other = ExpenditureAllocation(expenditureId, EnvelopeId(UUID(0L, 3L)), 1)

        assertEquals(12_345L, ExpenditureAllocationEntity.fromDomain(expenditure.allocations.single()).amount)
        assertEquals(1L, ExpenditureAllocationEntity.fromDomain(other).amount)
    }
}
