package cn.osmrpighead.envelog.data.query_old

import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.EntryEntity

data class AllocationResult(
    val transaction: EntryEntity,
    val allocation: ExpenditureAllocationEntity,
)