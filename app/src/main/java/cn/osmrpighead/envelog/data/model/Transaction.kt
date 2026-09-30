package cn.osmrpighead.envelog.data.model

import cn.osmrpighead.envelog.util.HasId
import java.time.ZonedDateTime
import java.util.UUID

enum class TransactionType {
    Expenditure,
}

data class Transaction(
    override val id: UUID,
    val type: TransactionType,
    val dateTime: ZonedDateTime,
    val amount: Long,
) : HasId
