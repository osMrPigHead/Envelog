package cn.osmrpighead.envelog.data.repository_old

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.EntryEntity

data class EnvelogSeedData(
    val envelopes: List<EnvelopeEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val transactions: List<EntryEntity> = emptyList(),
    val allocations: List<ExpenditureAllocationEntity> = emptyList(),
)
