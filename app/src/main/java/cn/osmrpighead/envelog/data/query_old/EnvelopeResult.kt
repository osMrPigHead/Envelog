package cn.osmrpighead.envelog.data.query_old

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity

data class EnvelopeResult(
    val envelope: EnvelopeEntity,
    val budgets: List<BudgetEntity>,
    val allocations: List<AllocationResult>,
)
