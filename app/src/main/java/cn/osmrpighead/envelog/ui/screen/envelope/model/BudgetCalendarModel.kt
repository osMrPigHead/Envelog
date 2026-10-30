package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import java.time.LocalDate

data class BudgetCalendarModel(
    val allocations: Map<LocalDate, Long>,
    val budgets: List<BudgetEntity>,
)