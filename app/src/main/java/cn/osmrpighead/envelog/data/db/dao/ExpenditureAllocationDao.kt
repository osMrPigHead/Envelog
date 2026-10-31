package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity

@Dao
interface ExpenditureAllocationDao {
    @Insert
    suspend fun insertAll(entities: List<ExpenditureAllocationEntity>)
}