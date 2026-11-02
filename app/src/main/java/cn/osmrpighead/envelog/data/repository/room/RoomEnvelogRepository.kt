package cn.osmrpighead.envelog.data.repository.room

import androidx.room3.withWriteTransaction
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.SpanMatchMode
import cn.osmrpighead.envelog.core.time.SpanMatchMode.*
import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.room.mapper.toEntity
import cn.osmrpighead.envelog.data.repository.room.mapper.toDomain
import cn.osmrpighead.envelog.util.toSetIfNonDuplicates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

class RoomEnvelogRepository @Inject constructor(
    private val db: EnvelogDatabase,
    private val clock: Clock,
) : EnvelogRepository {
    private val envelopeDao = db.envelopeDao()
    private val budgetDao = db.budgetDao()
    private val expenditureDao = db.expenditureDao()
    private val expenditureAllocationDao = db.expenditureAllocationDao()

    override fun observeOverview(
        today: LocalDate,
        yearMonth: YearMonth,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<EnvelopeSummary>> =
        envelopeDao.observeEnvelopeSummaries(today, yearMonth, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeAllocations(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<AllocationWithInfo>> =
        expenditureAllocationDao.observeAllocations(envelopeId, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeBudgets(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
        mode: SpanMatchMode,
    ): Flow<List<Budget>> = when (mode) {
        Overlaps -> budgetDao.observeBudgetOverlaps(envelopeId, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

        StartWithin -> TODO()
        EndWithin -> TODO()
        AttributedMonth -> TODO()
    }

    override fun observeDailyAllocationOverview(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<DailyAllocationSummary>> =
        expenditureAllocationDao.observeDailyAllocationSummaries(envelopeId, dateSpan.startDate, dateSpan.endDate)

    override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult =
        when (envelopeDao.archiveEnvelopeById(id, clock.instant())) {
            0 -> ArchiveResult.NotFound
            else -> ArchiveResult.Archived
        }

    override suspend fun recordExpenditure(expenditure: ExpenditureSubmission) {
        db.withWriteTransaction {
            require(expenditure.allocations.isNotEmpty())
            require(expenditure.happenedAt.toLocalDate(expenditure.happenedZone) == expenditure.happenedDate)
            require(expenditure.allocations.all { it.expenditureId == expenditure.id && it.amount > 0 })
            val envelopeIds = expenditure.allocations.map { it.envelopeId }.toSetIfNonDuplicates()
            requireNotNull(envelopeIds)
            val envelopes = envelopeDao.getByIds(envelopeIds)
            require(envelopes.all { it.archivedAt == null })

            expenditureDao.insert(expenditure.toEntity())
            expenditureAllocationDao.insertAll(expenditure.allocations.map { it.toEntity() })
        }
    }
}