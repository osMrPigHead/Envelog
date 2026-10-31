package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.model.ExpenditureId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "expenditure")
data class ExpenditureEntity(
    @PrimaryKey val id: ExpenditureId,
    val happenedAt: Instant,
    val happenedDate: LocalDate,
    val happenedZone: ZoneId,
    val deletedAt: Instant?,
) {
    companion object {
        fun fromDomain(domain: Expenditure): ExpenditureEntity = ExpenditureEntity(
            id = domain.id,
            happenedAt = domain.happenedAt,
            happenedDate = domain.happenedDate,
            happenedZone = domain.happenedZone,
            deletedAt = null,
        )
    }
}
