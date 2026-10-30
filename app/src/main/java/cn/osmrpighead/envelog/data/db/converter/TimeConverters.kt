package cn.osmrpighead.envelog.data.db.converter

import androidx.room3.ColumnTypeConverter
import java.time.LocalDate

class TimeConverters {
    @ColumnTypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @ColumnTypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}