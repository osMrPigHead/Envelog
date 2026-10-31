package cn.osmrpighead.envelog.data.repository

import androidx.room3.withWriteTransaction
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.db.dao.EnvelopeDao
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummaryRow
import cn.osmrpighead.envelog.util.toSetIfNonDuplicates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

// TODO: CurrencyType
private fun EnvelopeSummaryRow.toDomain() = if (isBudgeted) {
    EnvelopeSummary.Budgeted(
        id, name, allocated, CurrencyType.CNY, totalBudget ?: 0, currentBudget,
        currentSpanEndDate?.let { currentSpanStartDate?.untilExclusive(it) },
        currentSpanAllocated ?: 0,
        priorBudget ?: 0, priorAllocated ?: 0,
    )
} else EnvelopeSummary.NonBudget(id, name, allocated, CurrencyType.CNY)

class RoomEnvelogRepository @Inject constructor(
    private val db: EnvelogDatabase,
    private val clock: Clock,
) : EnvelogRepository {
    private val envelopeDao = db.envelopeDao()
    private val expenditureDao = db.expenditureDao()
    private val expenditureAllocationDao = db.expenditureAllocationDao()

    override fun observeOverview(
        today: LocalDate,
        yearMonth: YearMonth,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<EnvelopeSummary>> =
        envelopeDao.observeEnvelopeSummariesOverlaps(today, yearMonth, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult =
        when (envelopeDao.archiveEnvelopeById(id, clock.instant())) {
            0 -> ArchiveResult.NotFound
            else -> ArchiveResult.Archived
        }

    override suspend fun recordExpenditure(expenditure: Expenditure) {
        db.withWriteTransaction {
            require(expenditure.allocations.isNotEmpty())
            require(expenditure.happenedAt.toLocalDate(expenditure.happenedZone) == expenditure.happenedDate)
            require(expenditure.allocations.all { it.expenditureId == expenditure.id && it.amount > 0 })
            val envelopeIds = expenditure.allocations.map { it.envelopeId }.toSetIfNonDuplicates()
            requireNotNull(envelopeIds)
            val envelopes = envelopeDao.getByIds(envelopeIds)
            require(envelopes.all { it.archivedAt == null })

            expenditureDao.insert(ExpenditureEntity.fromDomain(expenditure))
            expenditureAllocationDao.insertAll(expenditure.allocations.map { ExpenditureAllocationEntity.fromDomain(it) })
        }
    }
}