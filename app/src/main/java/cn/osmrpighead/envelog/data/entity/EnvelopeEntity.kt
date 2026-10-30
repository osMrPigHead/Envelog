package cn.osmrpighead.envelog.data.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.util.UUID

@Entity(tableName = "envelopes")
data class EnvelopeEntity(
    @PrimaryKey override val id: UUID,
    val name: String,
    val isBudgeted: Boolean,
    val archived: Boolean,
) : HasId
