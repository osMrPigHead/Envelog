package cn.osmrpighead.envelog.data.converter

import androidx.room3.ColumnTypeConverter
import java.time.LocalDate

class DateConverters {
    @ColumnTypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @ColumnTypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}