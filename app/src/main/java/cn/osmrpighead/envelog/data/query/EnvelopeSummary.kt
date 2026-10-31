package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.EnvelopeId

sealed interface EnvelopeSummary {
    val id: EnvelopeId
    val name: String
    val allocated: Long

    data class Budgeted(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
        val totalBudget: Long,
        val currentBudget: Long,
        val currentSpan: HalfOpenDateSpan,
        val currentSpanAllocated: Long,
    ) : EnvelopeSummary {
        val currentAvailable = currentBudget - currentSpanAllocated
    }

    data class NonBudget(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
    ) : EnvelopeSummary
}
