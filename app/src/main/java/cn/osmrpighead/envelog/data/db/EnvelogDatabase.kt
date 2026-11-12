package cn.osmrpighead.envelog.data.db

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import cn.osmrpighead.envelog.data.db.converter.IdConverters
import cn.osmrpighead.envelog.data.db.converter.TimeConverters
import cn.osmrpighead.envelog.data.db.dao.BudgetDao
import cn.osmrpighead.envelog.data.db.dao.EnvelopeDao
import cn.osmrpighead.envelog.data.db.dao.ExpenseAllocationDao
import cn.osmrpighead.envelog.data.db.dao.ExpenseDao
import cn.osmrpighead.envelog.data.db.dao.SeedDao
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenseEntity

@Database(
    entities = [
        EnvelopeEntity::class,
        BudgetEntity::class,
        ExpenseEntity::class,
        ExpenseAllocationEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(TimeConverters::class, IdConverters::class)
abstract class EnvelogDatabase : RoomDatabase() {
    abstract fun envelopeDao(): EnvelopeDao
    abstract fun budgetDao(): BudgetDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseAllocationDao(): ExpenseAllocationDao
    abstract fun seedDao(): SeedDao
}