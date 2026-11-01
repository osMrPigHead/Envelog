package cn.osmrpighead.envelog.data.model

data class ExpenditureAllocation(
    val expenditureId: ExpenditureId,
    val envelopeId: EnvelopeId,
    val amount: Long,
)
