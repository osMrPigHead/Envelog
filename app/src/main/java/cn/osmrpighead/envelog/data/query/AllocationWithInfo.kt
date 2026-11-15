package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseMetaInfo

data class AllocationWithInfo(
    val expense: ExpenseMetaInfo,
    val envelopeId: EnvelopeId,
    val amount: Long,
)
