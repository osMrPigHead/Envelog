package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Query
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface BudgetDao {
    @Query(
        """
            SELECT *
            FROM budget
            WHERE deletedAt IS NULL
              AND budget.envelopeId = :envelopeId
              AND (budget.startDate < :spanEnd
               AND budget.endDate > :spanStart)
        """
    )
    fun observeBudgetOverlaps(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<BudgetEntity>>
}