package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ExpenditureAllocationDao {
    @Insert
    suspend fun insertAll(entities: List<ExpenditureAllocationEntity>)

    @Query(
        """
            SELECT ea.expenditureId, ea.envelopeId, ea.amount,
                   ex.happenedAt, ex.happenedDate, ex.happenedZone
            FROM expenditureAllocation ea
            JOIN expenditure ex
              ON ea.expenditureId = ex.id
             AND ex.happenedDate >= :spanStart
             AND ex.happenedDate < :spanEnd
            WHERE ea.envelopeId = :envelopeId
        """,
    )
    fun observeAllocations(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<AllocationWithInfoRow>>

    @Query(
        """
            SELECT ea.envelopeId, ex.happenedDate, SUM(ea.amount) as amount
            FROM expenditureAllocation ea
            JOIN expenditure ex
              ON ea.expenditureId = ex.id
             AND ex.happenedDate >= :spanStart
             AND ex.happenedDate < :spanEnd
            WHERE ea.envelopeId = :envelopeId
            GROUP BY ex.happenedDate
        """,
    )
    fun observeDailyAllocationSummaries(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<DailyAllocationSummary>>
}