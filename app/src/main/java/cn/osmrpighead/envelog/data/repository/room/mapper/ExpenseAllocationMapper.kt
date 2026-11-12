package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.data.db.entity.ExpenseAllocationEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.model.ExpenseAllocation
import cn.osmrpighead.envelog.data.model.ExpenseMetaInfo
import cn.osmrpighead.envelog.data.query.AllocationWithInfo

fun AllocationWithInfoRow.toDomain() = AllocationWithInfo(
    ExpenseMetaInfo(expenseId, happenedAt, happenedDate, happenedZone),
    envelopeId,
    amount,
)

fun ExpenseAllocation.toEntity() = ExpenseAllocationEntity(expenseId, envelopeId, amount)
