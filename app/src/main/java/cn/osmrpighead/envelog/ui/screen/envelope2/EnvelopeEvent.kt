package cn.osmrpighead.envelog.ui.screen.envelope2

import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import java.time.LocalDate
import java.time.YearMonth

sealed interface EnvelopeEvent

data class SelectMonth(val yearMonth: YearMonth) : EnvelopeEvent

data class InitializeNewEnvelope(val name: String) : EnvelopeEvent
data class UpdateEnvelope(val envelope: Envelope) : EnvelopeEvent
data object SaveEnvelope : EnvelopeEvent
data object ArchiveEnvelope : EnvelopeEvent
data object DeleteEnvelope : EnvelopeEvent
data object DismissEnvelopeFormError : EnvelopeEvent

data class OpenDetailWithExistingBudget(val id: BudgetId, val date: LocalDate?) : EnvelopeEvent
data class OpenDetailCreatingBudget(val date: LocalDate) : EnvelopeEvent
data class UpdateBudget(val budget: Budget) : EnvelopeEvent
data object SaveBudget : EnvelopeEvent
data object DeleteBudget : EnvelopeEvent
data object DismissDetail : EnvelopeEvent
data object ClearDetail : EnvelopeEvent
