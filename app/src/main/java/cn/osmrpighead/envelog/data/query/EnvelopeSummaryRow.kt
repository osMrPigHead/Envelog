package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.model.EnvelopeId
import java.time.LocalDate

data class EnvelopeSummaryRow(
    val id: EnvelopeId,
    val name: String,
    val isBudgeted: Boolean,
    val allocated: Long,
    val totalBudget: Long?,
    val currentBudget: Long?,
    val currentSpanStartDate: LocalDate?,
    val currentSpanEndDate: LocalDate?,
    val currentSpanAllocated: Long?,
    val priorBudget: Long?,
    val priorAllocated: Long?,
)
