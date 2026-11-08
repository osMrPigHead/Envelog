package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.UUIDGenerator
import cn.osmrpighead.envelog.util.UUIDSerializer
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
value class ExpenditureId(override val value: @Serializable(with = UUIDSerializer::class) UUID) : EntryId {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = ExpenditureId(UUIDGenerator.instance.generate())
    }
}

sealed interface Entry {
    val id: EntryId
    val happenedAt: Instant
    val happenedDate: LocalDate
    val happenedZone: ZoneId
}

data class ExpenditureSubmission(
    override val id: ExpenditureId,
    override val happenedAt: Instant,
    override val happenedDate: LocalDate,
    override val happenedZone: ZoneId,
    val allocations: List<ExpenditureAllocation>,
) : Entry

data class ExpenditureMetaInfo(
    override val id: ExpenditureId,
    override val happenedAt: Instant,
    override val happenedDate: LocalDate,
    override val happenedZone: ZoneId,
) : Entry
