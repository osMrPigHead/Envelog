package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.data.query_old.EnvelopeResult
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import java.time.YearMonth

sealed interface EnvelopeSheetUiState {
    data object Loading : EnvelopeSheetUiState
    data object NoSuchEnvelope: EnvelopeSheetUiState
    data class Success(
        val value: EnvelopeResult,
        val yearMonth: YearMonth,
        val calendarModel: BudgetCalendarModel,
        val fields: EnvelopeSheetFields,
        val budgetItem: BudgetItemSheetModel?,
    ) : EnvelopeSheetUiState
}