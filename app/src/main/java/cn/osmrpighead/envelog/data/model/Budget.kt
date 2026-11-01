package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

@JvmInline
@Serializable
value class BudgetId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()
}
