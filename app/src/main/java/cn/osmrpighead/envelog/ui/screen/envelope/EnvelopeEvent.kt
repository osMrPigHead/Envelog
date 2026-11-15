package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

sealed interface EnvelopeEvent

data class SelectMonth(val yearMonth: YearMonth) : EnvelopeEvent

data class InitializeNewEnvelope(val name: String) : EnvelopeEvent
data class UpdateEnvelope(val field: EnvelopeField) : EnvelopeEvent
data object SaveEnvelope : EnvelopeEvent
data object ArchiveEnvelope : EnvelopeEvent
data object DeleteEnvelope : EnvelopeEvent
data object DismissEnvelopeFormError : EnvelopeEvent

data class OpenDetailWithExistingBudget(val id: BudgetId, val date: LocalDate?) : EnvelopeEvent
data class OpenDetailCreatingBudget(val date: LocalDate) : EnvelopeEvent
data class UpdateBudget(val field: BudgetField) : EnvelopeEvent
data object SaveBudget : EnvelopeEvent
data object DeleteBudget : EnvelopeEvent
data object DismissDetail : EnvelopeEvent
data object ClearDetail : EnvelopeEvent

data object OpenQuickSet : EnvelopeEvent
data class UpdateQuickSet(val field: QuickSetField) : EnvelopeEvent
data class ApplyQuickSet(val overwrite: Boolean) : EnvelopeEvent
data object CancelOverwrite : EnvelopeEvent
data object DismissQuickSet : EnvelopeEvent
data object ClearQuickSet : EnvelopeEvent

sealed interface EnvelopeField {
    data class Name(val name: String) : EnvelopeField
    data class IsBudgeted(val isBudgeted: Boolean) : EnvelopeField
}

sealed interface BudgetField {
    data class DateSpan(val span: HalfOpenDateSpan) : BudgetField
    data class Amount(val amount: Long) : BudgetField
}

sealed interface QuickSetField {
    data class Mode(val mode: QuickSetMode) : QuickSetField
    data class TargetSpan(val span: ClosedDateSpan) : QuickSetField

    sealed interface Daily : QuickSetField {
        data class Interval(val days: Long) : Daily
        data class Amount(val amount: Long) : Daily
    }

    sealed interface Weekly : QuickSetField {
        data class DaySpan(val start: DayOfWeek, val end: DayOfWeek) : Weekly
        data class Amount(val amount: Long) : Weekly
    }

    sealed interface Monthly : QuickSetField {
        data class Amount(val amount: Long) : Monthly
    }
}
