package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.UUIDSerializer
import cn.osmrpighead.envelog.util.generateUUID
import kotlinx.serialization.Serializable
import java.util.*

@JvmInline
@Serializable
value class EnvelopeId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = EnvelopeId(generateUUID())
    }
}

data class Envelope(
    val id: EnvelopeId,
    val name: String,
    val isBudgeted: Boolean,
    val archived: Boolean,
)
