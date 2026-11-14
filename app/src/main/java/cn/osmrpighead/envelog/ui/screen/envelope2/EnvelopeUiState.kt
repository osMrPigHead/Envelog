package cn.osmrpighead.envelog.ui.screen.envelope2

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeSubmissionError
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetSchedule
import cn.osmrpighead.envelog.ui.screen.envelope2.model.QuickSetFormState
import java.time.DayOfWeek
import java.time.YearMonth

data class EnvelopeForm(
    val state: FormState<EnvelopeSubmissionError>,
    val envelope: Envelope,
    val isNew: Boolean,
    val isDirty: Boolean,
) {
    val saveBeforeQuit = isNew || isDirty
}

enum class BudgetFormError {
    EnvelopeArchived, BudgetOverlaps, AmountNonPositive,
}

data class BudgetForm(
    val state: FormState<BudgetFormError>,
    val budget: Budget,
    val isNew: Boolean,
    val isDirty: Boolean,
) {
    val saveBeforeQuit = isDirty && state is FormState.Active && state.error == null
}

sealed interface QuickSetFormError {
    data object EnvelopeArchived : QuickSetFormError
    data class BudgetOverlaps(val conflicts: List<Budget>) : QuickSetFormError
    data object AmountNonPositive : QuickSetFormError
    data object IntervalInvalid : QuickSetFormError
    data object CannotGenerate : QuickSetFormError
}

data class QuickSetFormModalState(
    val state: FormState<QuickSetFormError>,
    val config: QuickSetFormState,
    val schedule: QuickSetSchedule?,
    val dismissed: Boolean,
)

data class DetailModalState(
    val budgetForm: BudgetForm,
    val allocations: List<AllocationWithInfo>,
    val dismissed: Boolean,
)

data class LedgerState(
    val calendar: EnvelopeCalendarModel,
    val budgets: List<Budget>,
    val allocations: List<AllocationWithInfo>,
) {
    companion object {
        fun of(
            span: HalfOpenDateSpan,
            firstDayOfWeek: DayOfWeek,
            budgets: List<Budget>,
            allocations: List<AllocationWithInfo>,
            allocationSummaries: List<DailyAllocationSummary>,
        ) = LedgerState(
            calendar = EnvelopeCalendarModel.of(
                span = span,
                firstDayOfWeek = firstDayOfWeek,
                budgets = budgets,
                allocations = allocationSummaries.associateBy { it.happenedDate },
            ),
            budgets = budgets,
            allocations = allocations,
        )
    }
}

sealed interface EnvelopeDetail {
    data class Success(
        val ledgerState: LedgerState,
        val envelopeForm: EnvelopeForm,
        val detailModalState: DetailModalState?,
        val quickSetFormModalState: QuickSetFormModalState?,
        val isDeletable: Boolean,
    ) : EnvelopeDetail

    data object IsNew : EnvelopeDetail
    data object Loading : EnvelopeDetail
}

data class EnvelopeUiState(
    val yearMonth: YearMonth,
    val detail: EnvelopeDetail,
)
