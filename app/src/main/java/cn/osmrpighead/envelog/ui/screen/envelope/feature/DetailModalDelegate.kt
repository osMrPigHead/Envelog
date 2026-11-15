package cn.osmrpighead.envelog.ui.screen.envelope.feature

import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFieldError
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetForm
import cn.osmrpighead.envelog.ui.screen.envelope.BudgetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.DetailModalState
import cn.osmrpighead.envelog.util.withElementIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class DetailModalDelegate(
    private val isNew: Boolean,
    private val envelopeId: EnvelopeId,
    private val yearMonth: StateFlow<YearMonth>,
    /** 当前月预算列表：重叠校验与 openWithExistingBudget 查找 */
    private val budgets: StateFlow<List<Budget>>,
    private val allocations: StateFlow<List<AllocationWithInfo>>,
    private val draftStore: BudgetDraftStore,
    private val repository: EnvelogRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<DetailModalState?>(null)
    val state: StateFlow<DetailModalState?> = _state.asStateFlow()

    private val formStateOf: (DetailModalState) -> FormState<*>? =
        { if (it.dismissed) null else it.budgetForm.state }

    suspend fun awaitIdle() {
        _state.first { it?.budgetForm?.state != FormState.Busy }
    }

    fun openWithExistingBudget(id: BudgetId, date: LocalDate?) = _state.update { state ->
        if (state != null && state.budgetForm.state == FormState.Busy) return@update state
        val budget = budgets.value.find { it.id == id } ?: return@update state
        val allocations =
            if (date == null) allocations.value.filter { it.expense.happenedDate in budget.dateSpan }
            else allocations.value.filter { it.expense.happenedDate == date }
        DetailModalState(
            budgetForm = BudgetForm(
                state = FormState.Active(null),
                fieldErrors = emptySet(),
                budget = budget,
                isNew = false,
                isDirty = false,
            ),
            allocations = allocations,
            dismissed = false,
        )
    }

    fun openCreatingBudget(date: LocalDate) = _state.update { state ->
        if (state != null && state.budgetForm.state == FormState.Busy) return@update state
        val allocations = allocations.value.filter { it.expense.happenedDate == date }
        DetailModalState(
            budgetForm = BudgetForm(
                state = FormState.Active(null),
                fieldErrors = setOf(BudgetFieldError.AmountNonPositive),
                budget = Budget(
                    id = BudgetId.generate(),
                    envelopeId = envelopeId,
                    dateSpan = date.toHalfOpenDateSpan(),
                    attributedMonth = yearMonth.value,
                    amount = 0,
                ),
                isNew = true,
                isDirty = false,
            ),
            allocations = allocations,
            dismissed = false,
        )
    }

    fun update(budget: Budget) {
        _state.update {
            if (it == null) return@update it
            if (it.budgetForm.budget.id != budget.id) return@update it
            it.copy(
                budgetForm = it.budgetForm.copy(
                    state = FormState.Active(null),
                    fieldErrors = it.budgetForm.fieldErrors
                        .withElementIf(
                            BudgetFieldError.AmountNonPositive,
                            budget.amount <= 0,
                        )
                        .withElementIf(
                            BudgetFieldError.BudgetOverlaps,
                            budgets.value.any { b -> b.dateSpan overlaps budget.dateSpan && b.id != budget.id },
                        ),
                    budget = budget,
                    isDirty = true,
                ),
            )
        }
    }

    private fun requestForm(): BudgetForm? {
        val snapshot = _state.requestBusy(
            allowError = false,
            formStateOf = formStateOf,
            toBusy = { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Busy)) },
        ) ?: return null
        return snapshot.budgetForm
    }

    private fun releaseForm(transform: (DetailModalState) -> DetailModalState) =
        _state.releaseBusy(formStateOf, transform)

    fun save() {
        val form = requestForm() ?: return

        if (isNew) {
            draftStore.update { budgets ->
                if (form.isNew) budgets + form.budget
                else budgets.map { if (it.id == form.budget.id) form.budget else it }
            }
            releaseForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        } else scope.launch {
            val result =
                if (form.isNew) repository.insertBudget(form.budget)
                else repository.updateBudget(form.budget)
            releaseForm {
                it.copy(
                    budgetForm = it.budgetForm.copy(
                        state = when (result) {
                            BudgetWriteResult.EnvelopeArchived -> FormState.Active(BudgetFormError.EnvelopeArchived)
                            BudgetWriteResult.BudgetOverlaps -> FormState.Active(null)
                            BudgetWriteResult.Success -> FormState.Completed
                        },
                        fieldErrors = it.budgetForm.fieldErrors.withElementIf(
                            BudgetFieldError.BudgetOverlaps,
                            result == BudgetWriteResult.BudgetOverlaps,
                        ),
                    ),
                )
            }
        }
    }

    fun delete() {
        val form = requestForm() ?: return
        if (isNew) {
            draftStore.update { budgets ->
                if (form.isNew) budgets
                else budgets.filter { it.id != form.budget.id }
            }
            releaseForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        } else scope.launch {
            repository.deleteBudget(form.budget.id)
            releaseForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        }
    }

    fun dismiss() {
        _state.update {
            if (it == null) return@update it
            it.copy(dismissed = true)
        }
    }

    fun clear() {
        _state.value = null
    }
}
