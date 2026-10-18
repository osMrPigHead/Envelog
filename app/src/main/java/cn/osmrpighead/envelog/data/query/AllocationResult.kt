package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction

data class AllocationResult(
    val transaction: Transaction,
    val allocation: ExpenditureEnvelopeAllocation,
)