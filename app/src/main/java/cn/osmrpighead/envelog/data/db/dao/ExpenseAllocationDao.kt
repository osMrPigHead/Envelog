package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import cn.osmrpighead.envelog.data.db.entity.ExpenseAllocationEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenseAllocationDao {
    @Insert
    suspend fun insert(entities: List<ExpenseAllocationEntity>)

    @Query(
        """
            SELECT ea.expenseId, ea.envelopeId, ea.amount,
                   ex.happenedAt, ex.happenedDate, ex.happenedZone
            FROM expenseAllocation ea
            JOIN expense ex
              ON ex.deletedAt IS NULL
             AND ea.expenseId = ex.id
             AND ex.happenedDate >= :spanStart
             AND ex.happenedDate < :spanEnd
            WHERE ea.envelopeId = :envelopeId
        """,
    )
    fun observe(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<AllocationWithInfoRow>>

    @Query(
        """
            SELECT ea.envelopeId, ex.happenedDate, SUM(ea.amount) as amount
            FROM expenseAllocation ea
            JOIN expense ex
              ON ex.deletedAt IS NULL
             AND ea.expenseId = ex.id
             AND ex.happenedDate >= :spanStart
             AND ex.happenedDate < :spanEnd
            WHERE ea.envelopeId = :envelopeId
            GROUP BY ex.happenedDate
        """,
    )
    fun observeDailySummaries(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<DailyAllocationSummary>>

    @Query(
        """
            DELETE FROM expenseAllocation
            WHERE envelopeId = :envelopeId
              AND expenseId IN (
                  SELECT id FROM expense WHERE deletedAt IS NOT NULL
              )
        """,
    )
    suspend fun clean(envelopeId: EnvelopeId): Int
}