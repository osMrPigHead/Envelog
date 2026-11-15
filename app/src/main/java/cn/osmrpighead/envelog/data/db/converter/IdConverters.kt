package cn.osmrpighead.envelog.data.db.converter

import androidx.room3.ColumnTypeConverter
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseId
import java.util.UUID

class IdConverters {
    @ColumnTypeConverter
    fun fromEnvelopeId(value: EnvelopeId): String = value.toString()

    @ColumnTypeConverter
    fun toEnvelopeId(value: String): EnvelopeId = EnvelopeId(UUID.fromString(value))

    @ColumnTypeConverter
    fun fromBudgetId(value: BudgetId): String = value.toString()

    @ColumnTypeConverter
    fun toBudgetId(value: String): BudgetId = BudgetId(UUID.fromString(value))

    @ColumnTypeConverter
    fun fromExpenseId(value: ExpenseId): String = value.toString()

    @ColumnTypeConverter
    fun toExpenseId(value: String): ExpenseId = ExpenseId(UUID.fromString(value))
}