package cn.osmrpighead.envelog.ui.screen.overview.model

import cn.osmrpighead.envelog.data.model.CurrencyType
import cn.osmrpighead.envelog.util.Ratio
import java.time.LocalDate
import java.util.UUID

data class EnvelopeModel(
    val id: UUID,
    val name: String,
    val currency: CurrencyType,
    val usedAmount: Long,
    val budgetRelated: BudgetRelated?,
) {
    data class BudgetRelated(
        val budgetAmount: Long,
        val usedProportion: Ratio,
        val endDate: LocalDate?,
        val today: LocalDate,
        val currentAvailable: Long,
        val diffAmount: Long,
    )
}
