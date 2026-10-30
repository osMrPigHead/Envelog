package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.entity.BudgetEntity
import cn.osmrpighead.envelog.data.entity.EnvelopeEntity

data class EnvelopeResult(
    val envelope: EnvelopeEntity,
    val budgets: List<BudgetEntity>,
    val allocations: List<AllocationResult>,
)
