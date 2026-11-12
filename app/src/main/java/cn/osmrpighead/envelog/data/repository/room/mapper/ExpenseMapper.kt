package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.data.db.entity.ExpenseEntity
import cn.osmrpighead.envelog.data.model.ExpenseSubmission

fun ExpenseSubmission.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    happenedAt = happenedAt,
    happenedDate = happenedDate,
    happenedZone = happenedZone,
    deletedAt = null,
)