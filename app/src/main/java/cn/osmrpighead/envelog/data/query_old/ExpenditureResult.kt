package cn.osmrpighead.envelog.data.query_old

import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity

data class ExpenditureResult(
    val transaction: ExpenditureEntity,
    val allocations: List<ExpenditureAllocationEntity>,
)
