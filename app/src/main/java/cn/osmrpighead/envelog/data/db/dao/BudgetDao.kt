package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

@Dao
interface BudgetDao {
    @Query(
        """
            SELECT *
            FROM budget
            WHERE deletedAt IS NULL
              AND budget.envelopeId = :envelopeId
              AND budget.startDate < :spanEnd
              AND budget.endDate > :spanStart
        """,
    )
    fun observeBudgetOverlaps(
        envelopeId: EnvelopeId,
        spanStart: LocalDate,
        spanEnd: LocalDate,
    ): Flow<List<BudgetEntity>>

    @Query(
        """
            SELECT *
            FROM budget
            WHERE deletedAt IS NULL
              AND budget.envelopeId = :envelopeId
              AND budget.attributedMonth = :attributedMonth
        """,
    )
    fun observeBudgetAttributedMonth(
        envelopeId: EnvelopeId,
        attributedMonth: YearMonth,
    ): Flow<List<BudgetEntity>>

    @Update
    suspend fun update(entity: BudgetEntity)

    @Insert
    suspend fun insert(entity: BudgetEntity)

    @Insert
    suspend fun insert(entities: List<BudgetEntity>)
}