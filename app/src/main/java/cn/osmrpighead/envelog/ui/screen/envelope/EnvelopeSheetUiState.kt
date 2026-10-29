package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.ui.model.ModelTarget
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeSheetModel
import java.time.YearMonth

data class EnvelopeSheetUiState(
    val target: ModelTarget<EnvelopeSheetModel>,
    val yearMonth: YearMonth,
    val fields: EnvelopeSheetFields,
    val budgetItemSheetModel: BudgetItemSheetModel?,
)