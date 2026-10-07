package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation

data class EnvelopeResult(
    val envelope: Envelope,
    val budgets: List<Budget>,
    val allocations: List<ExpenditureEnvelopeAllocation>,
)
