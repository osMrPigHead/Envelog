package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.LocalDate

data class DailyAllocationSummary(
    val envelopeId: EnvelopeId,
    val happenedDate: LocalDate,
    val amount: Long,
)
