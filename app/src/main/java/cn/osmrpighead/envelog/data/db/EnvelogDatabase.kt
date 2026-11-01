package cn.osmrpighead.envelog.data.db

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import cn.osmrpighead.envelog.data.db.converter.IdConverters
import cn.osmrpighead.envelog.data.db.converter.TimeConverters
import cn.osmrpighead.envelog.data.db.dao.EnvelopeDao
import cn.osmrpighead.envelog.data.db.dao.ExpenditureAllocationDao
import cn.osmrpighead.envelog.data.db.dao.ExpenditureDao
import cn.osmrpighead.envelog.data.db.dao.SeedDao
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity

@Database(
    entities = [
        EnvelopeEntity::class,
        BudgetEntity::class,
        ExpenditureEntity::class,
        ExpenditureAllocationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(TimeConverters::class, IdConverters::class)
abstract class EnvelogDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun expenditureDao(): ExpenditureDao
    abstract fun expenditureAllocationDao(): ExpenditureAllocationDao
    abstract fun seedDao(): SeedDao
}