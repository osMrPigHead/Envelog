package cn.osmrpighead.envelog.ui.screen.overview.model

import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.core.number.Ratio
import java.time.LocalDate
import java.util.UUID

@Deprecated("")
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
