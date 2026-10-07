package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction

data class ExpenditureResult(
    val transaction: Transaction,
    val allocations: List<ExpenditureEnvelopeAllocation>,
)
