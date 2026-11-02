package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission

fun ExpenditureSubmission.toEntity(): ExpenditureEntity = ExpenditureEntity(
    id = id,
    happenedAt = happenedAt,
    happenedDate = happenedDate,
    happenedZone = happenedZone,
    deletedAt = null,
)