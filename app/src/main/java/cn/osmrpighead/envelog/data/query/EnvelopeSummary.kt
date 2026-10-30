package cn.osmrpighead.envelog.data.query

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
        val currentPeriodBudget: Long,
        val currentPeriodAllocated: Long,
    ) : EnvelopeSummary {
        val currentPeriodAvailable = currentPeriodBudget - currentPeriodAllocated
    }

    data class NonBudget(
        override val id: EnvelopeId,
        override val name: String,
        override val allocated: Long,
    ) : EnvelopeSummary
}
