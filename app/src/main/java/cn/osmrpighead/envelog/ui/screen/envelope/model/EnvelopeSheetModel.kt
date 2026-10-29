package cn.osmrpighead.envelog.ui.screen.envelope.model

import cn.osmrpighead.envelog.data.model.Envelope

data class EnvelopeSheetModel(
    val envelope: Envelope,
    val calendarModel: BudgetCalendarModel,
)
