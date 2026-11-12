package cn.osmrpighead.envelog.data.repository.room

import android.database.sqlite.SQLiteConstraintException
import androidx.room3.withWriteTransaction
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.hasOverlap
import cn.osmrpighead.envelog.data.db.EnvelogDatabase
import cn.osmrpighead.envelog.data.model.*
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.data.query.EnvelopeSummary
import cn.osmrpighead.envelog.data.repository.ArchiveResult
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.DeleteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult
import cn.osmrpighead.envelog.data.repository.EnvelopeUpdateResult
import cn.osmrpighead.envelog.data.repository.room.mapper.toDomain
import cn.osmrpighead.envelog.data.repository.room.mapper.toEntity
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
        envelopeDao.observeSummaries(today, yearMonth, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeAllocations(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<AllocationWithInfo>> =
        expenditureAllocationDao.observe(envelopeId, dateSpan.startDate, dateSpan.endDate)
            .map { rows -> rows.map { it.toDomain() } }

    override fun observeBudgets(
        envelopeId: EnvelopeId,
        attributedMonth: YearMonth,
    ): Flow<List<Budget>> = budgetDao.observe(envelopeId, attributedMonth)
        .map { rows -> rows.map { it.toDomain() } }

    override fun observeBudgets(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<Budget>> = budgetDao.observeOverlaps(envelopeId, dateSpan.startDate, dateSpan.endDate)
        .map { rows -> rows.map { it.toDomain() } }

    override fun observeDailyAllocationOverview(
        envelopeId: EnvelopeId,
        dateSpan: HalfOpenDateSpan,
    ): Flow<List<DailyAllocationSummary>> =
        expenditureAllocationDao.observeDailySummaries(envelopeId, dateSpan.startDate, dateSpan.endDate)

    override suspend fun getEnvelope(id: EnvelopeId): Envelope =
        envelopeDao.get(id)?.toDomain() ?: throw NoSuchElementException()

    override suspend fun updateEnvelope(envelope: Envelope): EnvelopeUpdateResult =
        if (envelopeDao.update(envelope.toEntity()) == 0) EnvelopeUpdateResult.NotFound
        else EnvelopeUpdateResult.Success

    override suspend fun insertEnvelope(envelope: Envelope, budgets: List<Budget>): BudgetWriteResult {
        if (budgets.map { it.dateSpan }.hasOverlap()) return BudgetWriteResult.BudgetOverlaps
        db.withWriteTransaction {
            envelopeDao.insert(envelope.toEntity())
            budgetDao.insert(budgets.map { it.toEntity() })
        }
        return BudgetWriteResult.Success
    }

    override suspend fun archiveEnvelope(id: EnvelopeId): ArchiveResult =
        when (envelopeDao.archive(id, clock.instant())) {
            0 -> ArchiveResult.NotFoundOrAlreadyArchived
            else -> ArchiveResult.Archived
        }

    override suspend fun deleteEnvelope(id: EnvelopeId): EnvelopeDeleteResult =
        try {
            if (db.withWriteTransaction {
                    expenditureDao.clean(id)
                    expenditureAllocationDao.clean(id)
                    budgetDao.clean(id)
                    envelopeDao.delete(id)
                } == 0
            ) EnvelopeDeleteResult.NotFound
            else EnvelopeDeleteResult.Deleted
        } catch (_: SQLiteConstraintException) {
            EnvelopeDeleteResult.Constraint
        }

    private suspend fun budgetOverlaps(budget: Budget) =
        budgetDao.countOverlaps(budget.envelopeId, budget.id, budget.dateSpan.startDate, budget.dateSpan.endDate) != 0L

    override suspend fun updateBudget(budget: Budget): BudgetWriteResult = db.withWriteTransaction {
        if (envelopeDao.getArchived(budget.envelopeId) != null)
            return@withWriteTransaction BudgetWriteResult.EnvelopeArchived
        if (budgetOverlaps(budget))
            return@withWriteTransaction BudgetWriteResult.BudgetOverlaps
        budgetDao.update(budget.normalize().toEntity())
        return@withWriteTransaction BudgetWriteResult.Success
    }

    override suspend fun insertBudget(budget: Budget): BudgetWriteResult = db.withWriteTransaction {
        if (envelopeDao.getArchived(budget.envelopeId) != null)
            return@withWriteTransaction BudgetWriteResult.EnvelopeArchived
        if (budgetOverlaps(budget))
            return@withWriteTransaction BudgetWriteResult.BudgetOverlaps
        budgetDao.insert(budget.normalize().toEntity())
        return@withWriteTransaction BudgetWriteResult.Success
    }

    override suspend fun replaceBudgets(
        envelopeId: EnvelopeId,
        deleteBudgetIds: Collection<BudgetId>,
        budgets: List<Budget>,
    ): BudgetWriteResult = db.withWriteTransaction {
        if (envelopeDao.getArchived(envelopeId) != null)
            return@withWriteTransaction BudgetWriteResult.EnvelopeArchived
        require(budgets.all { it.envelopeId == envelopeId }) {
            "All budgets must belong to the target envelope"
        }
        if (budgets.map { it.dateSpan }.hasOverlap())
            return@withWriteTransaction BudgetWriteResult.BudgetOverlaps
        val remainingOverlaps = budgets.any { budget ->
            budgetDao.getOverlaps(
                envelopeId = envelopeId,
                spanStart = budget.dateSpan.startDate,
                spanEnd = budget.dateSpan.endDate,
            ).any { it.id !in deleteBudgetIds }
        }
        if (remainingOverlaps)
            return@withWriteTransaction BudgetWriteResult.BudgetOverlaps
        if (deleteBudgetIds.isNotEmpty()) budgetDao.deleteAll(deleteBudgetIds, envelopeId, clock.instant())
        if (budgets.isNotEmpty()) budgetDao.insert(budgets.map { it.normalize().toEntity() })
        return@withWriteTransaction BudgetWriteResult.Success
    }

    override suspend fun deleteBudget(id: BudgetId): DeleteResult =
        when (budgetDao.delete(id, clock.instant())) {
            0 -> DeleteResult.NotFoundOrAlreadyDeleted
            else -> DeleteResult.Deleted
        }

    override suspend fun recordExpenditure(expenditure: ExpenditureSubmission) {
        db.withWriteTransaction {
            require(expenditure.allocations.isNotEmpty())
            require(expenditure.allocations.all { it.expenditureId == expenditure.id && it.amount > 0 })
            val envelopeIds = expenditure.allocations.map { it.envelopeId }.toSetIfNonDuplicates()
            requireNotNull(envelopeIds)
            val envelopes = envelopeDao.get(envelopeIds)
            require(envelopes.all { it.archivedAt == null })

            expenditureDao.insert(expenditure.toEntity())
            expenditureAllocationDao.insert(expenditure.allocations.map { it.toEntity() })
        }
    }
}