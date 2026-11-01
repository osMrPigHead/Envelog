package cn.osmrpighead.envelog.data.db.converter

import androidx.room3.ColumnTypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class TimeConverters {
    @ColumnTypeConverter
    fun fromLocalDate(value: LocalDate): String = value.toString()

    @ColumnTypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)

    @ColumnTypeConverter
    fun fromInstant(value: Instant): String = value.toString()

    @ColumnTypeConverter
    fun toInstant(value: String): Instant = Instant.parse(value)

    @ColumnTypeConverter
    fun fromYearMonth(value: YearMonth): String = value.toString()

    @ColumnTypeConverter
    fun toYearMonth(value: String): YearMonth = YearMonth.parse(value)

    @ColumnTypeConverter
    fun fromZoneId(value: ZoneId): String = value.id

    @ColumnTypeConverter
    fun toZoneId(value: String): ZoneId = ZoneId.of(value)
}