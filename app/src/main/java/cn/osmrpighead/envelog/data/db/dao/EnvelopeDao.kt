package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Update
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.EnvelopeSummaryRow
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface EnvelopeDao {
    @Query(
        """
            WITH totalBudget AS (
                SELECT envelopeId, SUM(amount) AS value
                FROM budget
                WHERE deletedAt IS NULL
                  AND startDate < :endDate
                  AND :startDate < endDate
                GROUP BY envelopeId
            ),
            totalAllocation AS (
                SELECT a.envelopeId, SUM(a.amount) AS value
                FROM expenditureAllocation a
                JOIN entry en ON en.id = a.entryId
                WHERE en.deletedAt IS NULL
                  AND :startDate <= en.happenedDate
                  AND en.happenedDate < :endDate
                GROUP BY a.envelopeId
            ),
            currentBudget AS (
                SELECT envelopeId, amount AS value, startDate, endDate
                FROM budget
                WHERE deletedAt IS NULL
                  AND startDate <= :today
                  AND :today < endDate
            ),
            currentAllocation AS (
                SELECT a.envelopeId, SUM(a.amount) AS value
                FROM expenditureAllocation a
                JOIN entry en ON en.id = a.entryId
                JOIN currentBudget cb ON cb.envelopeId = a.envelopeId
                WHERE en.deletedAt IS NULL
                  AND cb.startDate <= en.happenedDate
                  AND en.happenedDate < cb.endDate
                GROUP BY a.envelopeId
            )
            
            SELECT
                e.id, e.name, e.isBudgeted,
                COALESCE(totalAllocation.value, 0) AS allocated,
                totalBudget.value AS totalBudget,
                currentBudget.value AS currentBudget,
                currentBudget.startDate AS currentBudgetStartDate,
                currentBudget.endDate AS currentBudgetEndDate,
                COALESCE(currentAllocation.value, 0) AS currentBudgetAllocated
            FROM envelope e
            LEFT JOIN totalBudget       ON e.id = totalBudget.envelopeId
            LEFT JOIN currentBudget     ON e.id = currentBudget.envelopeId
            LEFT JOIN totalAllocation   ON e.id = totalAllocation.envelopeId
            LEFT JOIN currentAllocation ON e.id = currentAllocation.envelopeId
        """,
    )
    fun observeEnvelopeSummariesOverlaps(
        today: LocalDate,
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<List<EnvelopeSummaryRow>>

    @Query("SELECT * FROM envelope WHERE id = :id")
    suspend fun getById(id: EnvelopeId): EnvelopeEntity?

    @Query("SELECT * FROM envelope WHERE id IN (:ids)")
    suspend fun getByIds(ids: Set<EnvelopeId>): List<EnvelopeEntity>

    @Query("UPDATE envelope SET archivedAt = :now WHERE id = :id AND archivedAt IS NULL")
    suspend fun archiveEnvelopeById(id: EnvelopeId, now: Instant): Int

    @Update
    suspend fun updateEnvelope(entity: EnvelopeEntity)
}