package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.ui.model.Field
import cn.osmrpighead.envelog.ui.model.MutableField
import cn.osmrpighead.envelog.core.time.DatePeriod

data class BudgetItemSheetMutableFields(
    val period: MutableField<DatePeriod> = MutableField(),
    val amount: MutableField<Long> = MutableField(),
)

data class BudgetItemSheetFields(
    val period: Field<DatePeriod> = Field(),
    val amount: Field<Long> = Field(),
)

data class BudgetItemSheetModel(
    val value: BudgetEntity,
    val fields: BudgetItemSheetFields,
)