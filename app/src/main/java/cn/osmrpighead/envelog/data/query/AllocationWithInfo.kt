package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo

data class AllocationWithInfo(
    val expenditure: ExpenditureMetaInfo,
    val envelopeId: EnvelopeId,
    val amount: Long,
)
