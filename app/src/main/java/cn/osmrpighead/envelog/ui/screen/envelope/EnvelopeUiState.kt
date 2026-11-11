package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

enum class EnvelopeSubmissionError {
    BudgetOverlaps,
}

enum class ActiveBudgetError {
    EnvelopeArchived, BudgetOverlaps, AmountNonPositive,
}

enum class QuickSetError {
    EnvelopeArchived, BudgetOverlaps,
}

data class QuickSetUiState(
    val open: Boolean = false,
    val conflict: QuickSetBudgetPlan? = null,
    val error: QuickSetError? = null,
)

data class ActiveItem(
    val budget: Budget?,
    val isNew: Boolean,
    val error: ActiveBudgetError?,
    val complete: Boolean,
    val allocations: List<AllocationWithInfo> = emptyList(),
)

sealed interface EnvelopeUiState {
    data class Success(
        val envelope: Envelope,
        val yearMonth: YearMonth,
        val today: LocalDate,
        val budgets: Map<BudgetId, Budget>,
        val amountFieldId: Any,
        val allocations: List<DailyAllocationSummary>,
        val activeItem: ActiveItem,
        val envelopeSubmissionError: EnvelopeSubmissionError?,
        val quickSet: QuickSetUiState = QuickSetUiState(),
    ) : EnvelopeUiState {
        val allocationsByDate = allocations.associateBy { it.happenedDate }
        val calendar = EnvelopeCalendarModel.of(  // TODO: 优化
            span = yearMonth.toHalfOpenDateSpan(),
            firstDayOfWeek = DayOfWeek.MONDAY,  // TODO: User rule
            budgets = if (envelope.isBudgeted) budgets.values else null,
            allocations = allocationsByDate,
        )
    }

    data object IsNew : EnvelopeUiState
    data object Loading : EnvelopeUiState
}
