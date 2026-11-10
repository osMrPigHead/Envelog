package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.util.UUIDSerializer
import cn.osmrpighead.envelog.util.generateUUID
import kotlinx.serialization.Serializable
import java.time.YearMonth
import java.util.*

@JvmInline
@Serializable
value class BudgetId(val value: @Serializable(with = UUIDSerializer::class) UUID) {
    override fun toString(): String = value.toString()

    companion object {
        fun generate() = BudgetId(generateUUID())
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
