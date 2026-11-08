package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.util.UUIDGenerator
import cn.osmrpighead.envelog.util.UUIDSerializer
import kotlinx.serialization.Serializable
import java.time.YearMonth
import java.util.UUID

@JvmInline
@Serializable
value class BudgetId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = BudgetId(UUIDGenerator.instance.generate())
    }
}

data class Budget(
    val id: BudgetId,
    val envelopeId: EnvelopeId,
    val dateSpan: HalfOpenDateSpan,
    val attributedMonth: YearMonth,
    val amount: Long,
) {
    fun normalize() = copy(attributedMonth = dateSpan.attributedMonth)
}
