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
    val canDelete: Boolean

    data class Budgeted(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
        override val currency: CurrencyType,
        override val archived: Boolean,
        override val canDelete: Boolean,
        val totalBudget: Long,
        val currentBudget: Long?,
        val currentSpan: HalfOpenDateSpan?,
        val currentSpanAllocated: Long,
        val priorBudget: Long,
        val priorAllocated: Long,
    ) : EnvelopeSummary {
        val currentAvailable: Long
        val priorDifference: Long
        val usedProportion = Ratio.of(allocated, totalBudget)

        init {
            val realCurrent = (currentBudget ?: 0) - currentSpanAllocated
            val realPrior = priorBudget - priorAllocated
            if (realCurrent < 0) {
                if (-realCurrent < realPrior) {
                    currentAvailable = 0
                    priorDifference = realPrior + realCurrent
                } else {
                    currentAvailable = realPrior + realCurrent
                    priorDifference = 0
                }
            } else {
                currentAvailable = realCurrent
                priorDifference = realPrior
            }
        }
    }

    data class NonBudget(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
        override val currency: CurrencyType,
        override val archived: Boolean,
        override val canDelete: Boolean,
    ) : EnvelopeSummary
}
