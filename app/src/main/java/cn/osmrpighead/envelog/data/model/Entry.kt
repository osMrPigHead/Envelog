package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.core.time.toLocalDate
import cn.osmrpighead.envelog.util.UUIDSerializer
import cn.osmrpighead.envelog.util.generateUUID
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Serializable
sealed interface EntryId {
    val value: @Serializable(with = UUIDSerializer::class) UUID
}

@JvmInline
@Serializable
value class ExpenseId(override val value: @Serializable(with = UUIDSerializer::class) UUID) : EntryId {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = ExpenseId(generateUUID())
    }
}

sealed interface Entry {
    val id: EntryId
    val happenedAt: Instant
    val happenedDate: LocalDate
    val happenedZone: ZoneId
}

data class ExpenseSubmission(
    override val id: ExpenseId,
    override val happenedAt: Instant,
    override val happenedZone: ZoneId,
    val allocations: List<ExpenseAllocation>,
) : Entry {
    override val happenedDate: LocalDate = happenedAt.toLocalDate(happenedZone)
}

data class ExpenseMetaInfo(
    override val id: ExpenseId,
    override val happenedAt: Instant,
    override val happenedDate: LocalDate,
    override val happenedZone: ZoneId,
) : Entry
