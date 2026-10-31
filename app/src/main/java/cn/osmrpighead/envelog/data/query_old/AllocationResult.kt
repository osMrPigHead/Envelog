package cn.osmrpighead.envelog.data.query_old

import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity

data class AllocationResult(
    val transaction: ExpenditureEntity,
    val allocation: ExpenditureAllocationEntity,
)