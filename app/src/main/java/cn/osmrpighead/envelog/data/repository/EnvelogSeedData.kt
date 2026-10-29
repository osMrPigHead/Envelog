package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction

data class EnvelogSeedData(
    val envelopes: List<Envelope> = emptyList(),
    val budgets: List<Budget> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val allocations: List<ExpenditureEnvelopeAllocation> = emptyList(),
)
