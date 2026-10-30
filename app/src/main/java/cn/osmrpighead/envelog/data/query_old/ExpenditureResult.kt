package cn.osmrpighead.envelog.data.query_old

import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.EntryEntity

data class ExpenditureResult(
    val transaction: EntryEntity,
    val allocations: List<ExpenditureAllocationEntity>,
)
