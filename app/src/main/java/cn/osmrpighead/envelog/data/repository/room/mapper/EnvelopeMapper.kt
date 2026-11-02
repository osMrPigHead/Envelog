package cn.osmrpighead.envelog.data.repository.room.mapper

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.row.EnvelopeSummaryRow
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.EnvelopeSummary

// TODO: CurrencyType
fun EnvelopeSummaryRow.toDomain() = if (isBudgeted) {
    EnvelopeSummary.Budgeted(
        id, name, allocated, CurrencyType.CNY, archived, totalBudget ?: 0, currentBudget,
        currentSpanEndDate?.let { currentSpanStartDate?.untilExclusive(it) },
        currentSpanAllocated ?: 0,
        priorBudget ?: 0, priorAllocated ?: 0,
    )
} else EnvelopeSummary.NonBudget(id, name, allocated, CurrencyType.CNY, archived)

fun EnvelopeEntity.toDomain() = Envelope(id, name, isBudgeted, archivedAt != null)
fun Envelope.toEntity() =
    EnvelopeEntity(id, name, isBudgeted, if (!archived) null else throw IllegalArgumentException())
