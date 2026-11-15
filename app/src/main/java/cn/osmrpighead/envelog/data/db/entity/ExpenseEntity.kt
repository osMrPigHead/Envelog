package cn.osmrpighead.envelog.data.db.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.data.model.ExpenseId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "expense")
data class ExpenseEntity(
    @PrimaryKey val id: ExpenseId,
    val happenedAt: Instant,
    val happenedDate: LocalDate,
    val happenedZone: ZoneId,
    val deletedAt: Instant?,
)
