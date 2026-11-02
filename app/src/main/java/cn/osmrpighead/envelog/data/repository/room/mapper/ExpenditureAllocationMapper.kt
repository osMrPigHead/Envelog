package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.row.AllocationWithInfoRow
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo
import cn.osmrpighead.envelog.data.query.AllocationWithInfo

fun AllocationWithInfoRow.toDomain() = AllocationWithInfo(
    ExpenditureMetaInfo(expenditureId, happenedAt, happenedDate, happenedZone),
    envelopeId,
    amount,
)

fun ExpenditureAllocation.toEntity() = ExpenditureAllocationEntity(expenditureId, envelopeId, amount)
