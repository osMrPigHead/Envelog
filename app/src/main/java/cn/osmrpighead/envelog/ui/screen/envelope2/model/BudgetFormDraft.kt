package cn.osmrpighead.envelog.ui.screen.envelope2.model

import cn.osmrpighead.envelog.util.date.ClosedDatePeriod
import java.util.UUID

data class BudgetFormDraft(
    val id: UUID,
    val period: ClosedDatePeriod,
    val amount: Long,
)