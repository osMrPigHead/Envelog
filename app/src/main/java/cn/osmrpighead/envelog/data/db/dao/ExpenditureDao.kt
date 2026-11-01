package cn.osmrpighead.envelog.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity

@Dao
interface ExpenditureDao {
    @Insert
    suspend fun insert(entity: ExpenditureEntity)
}