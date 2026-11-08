package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.db.row.EnvelopeSummaryRow
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

@Dao
interface EnvelopeDao {
    @Query(
        """
            WITH budgetAgg AS (
                SELECT envelopeId,
                       SUM(amount) AS total,
                       MIN(startDate) AS minStart,
                       MAX(endDate) AS maxEnd
                FROM budget
                WHERE deletedAt IS NULL
                  AND attributedMonth = :yearMonth
                GROUP BY envelopeId
            ),
            currentBudget AS (
                SELECT envelopeId, SUM(amount) as amount, startDate, endDate
                FROM budget
                WHERE deletedAt IS NULL
                  AND :today >= startDate
                  AND :today < endDate
                GROUP BY envelopeId
            ),
            priorBudget AS ( -- TODO: 是否与budgetAgg合并 需要验证
                SELECT envelopeId, SUM(amount) AS total
                FROM budget
                WHERE deletedAt IS NULL
                  AND attributedMonth = :yearMonth
                  AND endDate <= :today
                GROUP BY envelopeId
            ),
            allocated AS (
                SELECT ea.envelopeId, SUM(ea.amount) AS total
                FROM expenditureAllocation ea
                LEFT JOIN budgetAgg ba ON ea.envelopeId = ba.envelopeId
                JOIN expenditure ex
                  ON ex.deletedAt IS NULL
                 AND ea.expenditureId = ex.id
                 AND ex.happenedDate >= COALESCE(MIN(ba.minStart, :spanStart), :spanStart)
                 AND ex.happenedDate < COALESCE(MAX(ba.maxEnd, :spanEnd), :spanEnd)
                GROUP BY ea.envelopeId
            ),
            currentAllocated AS (
                SELECT ea.envelopeId, SUM(ea.amount) AS total
                FROM expenditureAllocation ea
                JOIN currentBudget cb ON ea.envelopeId = cb.envelopeId
                JOIN expenditure ex
                  ON ex.deletedAt IS NULL
                 AND ea.expenditureId = ex.id
                 AND ex.happenedDate >= cb.startDate
                 AND ex.happenedDate < cb.endDate
                GROUP BY ea.envelopeId
            ),
            priorAllocated AS (
                SELECT ea.envelopeId, SUM(ea.amount) AS total
                FROM expenditureAllocation ea
                LEFT JOIN budgetAgg ba ON ea.envelopeId = ba.envelopeId
                LEFT JOIN currentBudget cb ON ea.envelopeId = cb.envelopeId
                JOIN expenditure ex
                  ON ex.deletedAt IS NULL
                 AND ea.expenditureId = ex.id
                 AND ex.happenedDate >= COALESCE(MIN(ba.minStart, :spanStart), :spanStart)
                 AND ex.happenedDate < COALESCE(cb.startDate, :today)
                GROUP BY ea.envelopeId
            )

            SELECT
                ev.id, ev.name, ev.isBudgeted, (ev.archivedAt IS NOT NULL) AS archived,
                ac.total AS allocated,
                ba.total AS totalBudget,
                cb.amount AS currentBudget,
                cb.startDate AS currentSpanStartDate,
                cb.endDate AS currentSpanEndDate,
                ca.total AS currentSpanAllocated,
                pb.total AS priorBudget,
                pa.total AS priorAllocated
            FROM envelope ev                
            LEFT JOIN allocated          ac ON ev.id = ac.envelopeId
            LEFT JOIN budgetAgg          ba ON ev.id = ba.envelopeId
            LEFT JOIN currentBudget      cb ON ev.id = cb.envelopeId
            LEFT JOIN currentAllocated   ca ON ev.id = ca.envelopeId
            LEFT JOIN priorBudget        pb ON ev.id = pb.envelopeId
            LEFT JOIN priorAllocated     pa ON ev.id = pa.envelopeId
        """,
    )
    fun observeEnvelopeSummaries(
        today: LocalDate,
        yearMonth: YearMonth,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<EnvelopeSummaryRow>>

    @Query("SELECT * FROM envelope WHERE id = :id")
    suspend fun getById(id: EnvelopeId): EnvelopeEntity?

    @Query("SELECT * FROM envelope WHERE id IN (:ids)")
    suspend fun getByIds(ids: Set<EnvelopeId>): List<EnvelopeEntity>

    @Query("UPDATE envelope SET archivedAt = :now WHERE id = :id AND archivedAt IS NULL")
    suspend fun archiveEnvelopeById(id: EnvelopeId, now: Instant): Int

    @Update
    suspend fun update(entity: EnvelopeEntity)

    @Insert
    suspend fun insert(entity: EnvelopeEntity)
}