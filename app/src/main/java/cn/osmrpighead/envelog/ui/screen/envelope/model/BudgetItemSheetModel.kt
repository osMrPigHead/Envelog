package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.model.FieldWithState
import cn.osmrpighead.envelog.ui.model.ModelTarget
import cn.osmrpighead.envelog.util.date.DatePeriod
import java.time.LocalDate

data class BudgetItemSheetFields(
    val period: FieldWithState<DatePeriod> = FieldWithState(),
    val amount: FieldWithState<Long> = FieldWithState(),
)

data class BudgetItemSheetModel(
    val target: ModelTarget<Budget>,
    val fields: BudgetItemSheetFields,
    val today: LocalDate,
)