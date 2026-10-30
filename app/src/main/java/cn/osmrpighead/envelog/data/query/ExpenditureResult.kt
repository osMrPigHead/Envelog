package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.entity.TransactionEntity

data class ExpenditureResult(
    val transaction: TransactionEntity,
    val allocations: List<ExpenditureAllocationEntity>,
)
