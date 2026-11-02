package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.data.model.Budget

fun BudgetEntity.toDomain() = Budget(id, envelopeId, dateSpan, attributedMonth, amount)
