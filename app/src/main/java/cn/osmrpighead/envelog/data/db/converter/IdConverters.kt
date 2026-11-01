package cn.osmrpighead.envelog.data.db.converter

import androidx.room3.ColumnTypeConverter
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
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
    fun fromExpenditureId(value: ExpenditureId): String = value.toString()

    @ColumnTypeConverter
    fun toExpenditureId(value: String): ExpenditureId = ExpenditureId(UUID.fromString(value))
}