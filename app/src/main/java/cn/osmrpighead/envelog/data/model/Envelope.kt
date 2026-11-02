package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@JvmInline
@Serializable
value class EnvelopeId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()
}

data class Envelope(
    val id: EnvelopeId,
    val name: String,
    val isBudgeted: Boolean,
    val archived: Boolean,
)
