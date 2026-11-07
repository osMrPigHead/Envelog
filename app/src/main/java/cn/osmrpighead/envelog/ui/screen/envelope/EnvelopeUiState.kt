package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel

sealed interface EnvelopeUiState {
    data class Success(
        val envelope: Envelope,
        val budgets: Map<BudgetId, Budget>,
        val activeBudget: Budget?,
        val calendar: BudgetCalendarModel,
    ) : EnvelopeUiState

    data object Loading : EnvelopeUiState
}
