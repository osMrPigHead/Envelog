package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.entity.TransactionEntity

data class AllocationResult(
    val transaction: TransactionEntity,
    val allocation: ExpenditureAllocationEntity,
)