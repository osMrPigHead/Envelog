package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetPreview
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class EnvelopeSubmissionError {
    BudgetOverlaps,
}

data class EnvelopeForm(
    val state: FormState<EnvelopeSubmissionError>,
    val envelope: Envelope,
    val isNew: Boolean,
    val isDirty: Boolean,
) {
    val saveBeforeQuit = isNew || isDirty
}

enum class BudgetFormError {
    EnvelopeArchived,
}

enum class BudgetFieldError {
    BudgetOverlaps, AmountNonPositive,
}

data class BudgetForm(
    val state: FormState<BudgetFormError>,
    val fieldErrors: Set<BudgetFieldError>,
    val budget: Budget,
    val isNew: Boolean,
    val isDirty: Boolean,
) {
    val isError = (state as? FormState.Active)?.error != null || fieldErrors.isNotEmpty()
    val saveBeforeQuit = isDirty && state is FormState.Active && !isError
}

enum class QuickSetFormError {
    EnvelopeArchived, CannotGenerate,
}

enum class QuickSetFieldError {
    AmountNonPositive, IntervalInvalid,
}

data class QuickSetFormModalState(
    val state: FormState<QuickSetFormError>,
    val fieldErrors: Set<QuickSetFieldError>,
    val config: QuickSetFormState,
    val schedule: QuickSetSchedule?,
    val pendingOverwrite: List<Budget>?,
    val dismissed: Boolean,
) {
    val isError = (state as? FormState.Active)?.error != null || fieldErrors.isNotEmpty()
    val preview: QuickSetPreview?

    init {
        val plan = schedule
        val amount = config.selectedConfig.amount
        preview =
            if (amount <= 0L || plan == null) null
            else {
                val days = plan.segments.sumOf { it.span.length }.toInt()
                when (config.mode) {
                    QuickSetMode.Daily -> QuickSetPreview.Daily(
                        intervalDays = config.dailyConfig.intervalDays,
                        amount = amount,
                        days = days,
                        totalAmount = plan.totalAmount,
                    )

                    QuickSetMode.Weekly -> QuickSetPreview.Weekly(
                        startDayOfWeek = config.weeklyConfig.startDayOfWeek,
                        endDayOfWeek = config.weeklyConfig.endDayOfWeek,
                        amount = amount,
                        days = days,
                        totalAmount = plan.totalAmount,
                    )

                    QuickSetMode.Monthly -> QuickSetPreview.MonthlyTotal(
                        days = days,
                        totalAmount = plan.totalAmount,
                    )
                }
            }
    }
}

data class DetailModalState(
    val budgetForm: BudgetForm,
    val dateSpan: ClosedDateSpan,
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
            isBudgeted: Boolean,
            budgets: List<Budget>,
            allocations: List<AllocationWithInfo>,
            allocationSummaries: List<DailyAllocationSummary>,
        ) = LedgerState(
            calendar = EnvelopeCalendarModel.of(
                span = span,
                firstDayOfWeek = firstDayOfWeek,
                isBudgeted = isBudgeted,
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
    val today: LocalDate,
    val detail: EnvelopeDetail,
)
