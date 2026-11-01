package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.number.Ratio
import cn.osmrpighead.envelog.data.model.EnvelopeId

sealed interface EnvelopeSummary {
    val id: EnvelopeId
    val name: String
    val allocated: Long
    val currency: CurrencyType
    val archived: Boolean

    data class Budgeted(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
        override val currency: CurrencyType,
        override val archived: Boolean,
        val totalBudget: Long,
        val currentBudget: Long?,
        val currentSpan: HalfOpenDateSpan?,
        val currentSpanAllocated: Long,
        val priorBudget: Long,
        val priorAllocated: Long,
    ) : EnvelopeSummary {
        val currentAvailable = currentBudget?.minus(currentSpanAllocated) ?: 0
        val usedProportion = Ratio.of(allocated, totalBudget)
        val priorDifference = priorBudget - priorAllocated
    }

    data class NonBudget(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
        override val currency: CurrencyType,
        override val archived: Boolean,
    ) : EnvelopeSummary
}
