package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.EnvelopeId

@Dao
interface ExpenditureDao {
    @Insert
    suspend fun insert(entity: ExpenditureEntity)

    // 必须在 ExpenditureAllocationDao.clean 之前调用：删除后无法再判定支出原本是否属于该信封
    @Query(
        """
            DELETE FROM expenditure
            WHERE deletedAt IS NOT NULL
              AND id IN (
                  SELECT expenditureId FROM expenditureAllocation WHERE envelopeId = :envelopeId
              )
              AND id NOT IN (
                  SELECT expenditureId FROM expenditureAllocation WHERE envelopeId <> :envelopeId
              )
        """,
    )
    suspend fun clean(envelopeId: EnvelopeId): Int
}