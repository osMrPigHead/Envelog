package cn.osmrpighead.envelog.data.db.row

import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenditureId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class AllocationWithInfoRow(
    val expenditureId: ExpenditureId,
    val happenedAt: Instant,
    val happenedDate: LocalDate,
    val happenedZone: ZoneId,
    val envelopeId: EnvelopeId,
    val amount: Long,
)