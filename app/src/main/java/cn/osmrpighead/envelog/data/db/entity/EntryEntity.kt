package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.data.model.EntryId
import java.time.Instant
import java.time.LocalDate

enum class EntryType {
    Expenditure,
}

@Entity(tableName = "entry")
data class EntryEntity(
    @PrimaryKey val id: EntryId,
    val type: EntryType,
    val happenedAt: Instant,
    val happenedDate: LocalDate,
    val deletedAt: Instant?,
)
