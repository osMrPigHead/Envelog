package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity

@Dao
interface SeedDao {
    @Query("SELECT COUNT(*) FROM envelope")
    suspend fun envelopeCount(): Int

    @Insert
    suspend fun insertEnvelopes(entities: List<EnvelopeEntity>)

    @Insert
    suspend fun insertBudgets(entities: List<BudgetEntity>)

    @Insert
    suspend fun insertExpenditures(entities: List<ExpenditureEntity>)

    @Insert
    suspend fun insertAllocations(entities: List<ExpenditureAllocationEntity>)
}
