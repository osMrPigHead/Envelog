package cn.osmrpighead.envelog.data.model

import java.util.UUID

data class ExpenditureEnvelopeAllocation(
    val transactionId: UUID,
    val envelopeId: UUID,
    val amount: Long,
)
