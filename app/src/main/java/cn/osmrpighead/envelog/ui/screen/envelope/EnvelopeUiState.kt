package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.DailyAllocationSummary
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import java.time.DayOfWeek
import java.time.YearMonth

enum class EnvelopeSubmissionError {
    BudgetOverlaps,
}

enum class ActiveError {
    EnvelopeArchived, BudgetOverlaps,
}

data class ActiveBudget(
    val budget: Budget?,
    val error: ActiveError?,
    val complete: Boolean,
)

sealed interface EnvelopeUiState {
    data class Success(
        val envelope: Envelope,
        val yearMonth: YearMonth,
        val budgets: Map<BudgetId, Budget>,
        val amountFieldId: Any,
        val allocations: List<DailyAllocationSummary>,
        val activeBudget: ActiveBudget,
        val envelopeSubmissionError: EnvelopeSubmissionError?,
    ) : EnvelopeUiState {
        val allocationsByDate = allocations.associateBy { it.happenedDate }
        val calendar = BudgetCalendarModel.of(  // TODO: 优化
            span = yearMonth.toHalfOpenDateSpan(),
            firstDayOfWeek = DayOfWeek.MONDAY,  // TODO: User rule
            budgets = budgets.values,
            allocations = allocationsByDate,
        )
    }

    data object IsNew : EnvelopeUiState
    data object Loading : EnvelopeUiState
}
