package cn.osmrpighead.envelog.data.repository

import cn.osmrpighead.envelog.data.entity.BudgetEntity
import cn.osmrpighead.envelog.data.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.entity.TransactionEntity

data class EnvelogSeedData(
    val envelopes: List<EnvelopeEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val allocations: List<ExpenditureAllocationEntity> = emptyList(),
)
