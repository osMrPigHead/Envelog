package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel

data class EnvelopeUiState(
    val envelope: Envelope,
    val calendar: BudgetCalendarModel
)
