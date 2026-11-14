package cn.osmrpighead.envelog.ui.screen.envelope2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult.*
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult.*
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeSubmissionError
import cn.osmrpighead.envelog.util.updateNotNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@HiltViewModel(assistedFactory = EnvelopeViewModel.Factory::class)
class EnvelopeViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Envelope,
    private val appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    fun selectMonth(yearMonth: YearMonth) = appState.selectMonth(yearMonth)

    private val isNew = route.id is NavigationTarget.New
    private val envelopeId = if (route.id is NavigationTarget.Existing) route.id.value else EnvelopeId.generate()

    private val memoryBudgets = MutableStateFlow(emptyList<Budget>())

    private val isDeletable =
        if (route.id is NavigationTarget.Existing) repository.observeEnvelopeDeletable(route.id.value)
        else flowOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val budgets = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeBudgets(
            envelopeId = route.id.value,
            dateSpan = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) else memoryBudgets

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allocations = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeAllocations(
            envelopeId = route.id.value,
            dateSpan = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) else MutableStateFlow(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allocationSummaries = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeDailyAllocationOverview(
            envelopeId = route.id.value,
            dateSpan = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
        )
    } else flowOf(emptyList())

    private val ledgerState = combine(
        yearMonth,
        budgets,
        allocations,
        allocationSummaries,
    ) { yearMonth, budgets, allocations, allocationSummaries ->
        LedgerState.of(
            span = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
            firstDayOfWeek = DayOfWeek.MONDAY, // TODO: User rule
            budgets = budgets,
            allocations = allocations,
            allocationSummaries = allocationSummaries,
        )
    }

    private val envelopeForm = MutableStateFlow<EnvelopeForm?>(null)
    private val detailModalState = MutableStateFlow<DetailModalState?>(null)
    private val quickSetFormModalState = MutableStateFlow<QuickSetFormModalState?>(null)

    private val detailNotReady = when (route.id) {
        is NavigationTarget.Existing -> EnvelopeDetail.Loading
        is NavigationTarget.New -> EnvelopeDetail.IsNew
    }
    private val detail = combine(
        ledgerState,
        envelopeForm,
        detailModalState,
        quickSetFormModalState,
        isDeletable,
    ) { ledgerState, envelopeForm, detailModalState, quickSetFormModalState, isDeletable ->
        if (envelopeForm == null) detailNotReady
        else EnvelopeDetail.Success(ledgerState, envelopeForm, detailModalState, quickSetFormModalState, isDeletable)
    }
    val uiState = combine(yearMonth, detail) { yearMonth, detail -> EnvelopeUiState(yearMonth, detail) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EnvelopeUiState(yearMonth.value, detailNotReady),
        )

    init {
        if (route.id is NavigationTarget.Existing) viewModelScope.launch {
            envelopeForm.value = EnvelopeForm(
                state = FormState.Active(null),
                envelope = repository.getEnvelope(route.id.value),
                isNew = false,
                isDirty = false,
            )
        }
    }

    fun initializeNewEnvelope(name: String) {
        envelopeForm.value = EnvelopeForm(
            state = FormState.Active(null),
            envelope = Envelope(
                id = envelopeId,
                name = name,
                isBudgeted = true,
                archived = false,
            ),
            isNew = true,
            isDirty = false,
        )
    }

    // TODO: 名称校验
    fun updateEnvelope(envelope: Envelope) = envelopeForm.updateNotNull {
        it.copy(envelope = envelope, isDirty = false)
    }

    private fun requestEnvelopeForm(): EnvelopeForm? {
        val form = envelopeForm.value ?: return null
        var active = false
        envelopeForm.update {
            if (it == null || it.state !is FormState.Active) return@update it
            active = true
            it.copy(state = FormState.Busy)
        }
        if (!active) return null
        return form
    }

    private fun releaseEnvelopeForm(function: (EnvelopeForm) -> EnvelopeForm) = envelopeForm.update {
        if (it == null || it.state !is FormState.Busy) return@update it
        function(it)
    }

    fun saveEnvelope() {
        val form = requestEnvelopeForm() ?: return

        viewModelScope.launch {
            detailModalState.first { it?.budgetForm?.state != FormState.Busy }
            if (!isNew) {
                repository.updateEnvelope(form.envelope)
                releaseEnvelopeForm { it.copy(state = FormState.Completed) }
                return@launch
            }
            val result = repository.insertEnvelope(form.envelope, budgets.value)
            releaseEnvelopeForm {
                it.copy(
                    state = when (result) {
                        EnvelopeArchived -> throw IllegalArgumentException()
                        BudgetOverlaps -> FormState.Active(EnvelopeSubmissionError.BudgetOverlaps)
                        Success -> FormState.Completed
                    },
                )
            }
        }
    }

    fun archiveEnvelope() {
        val form = requestEnvelopeForm() ?: return
        if (isNew) throw IllegalArgumentException()
        viewModelScope.launch {
            repository.archiveEnvelope(form.envelope.id)
            releaseEnvelopeForm { it.copy(state = FormState.Completed) }
        }
    }

    fun deleteEnvelope() {
        val form = requestEnvelopeForm() ?: return
        if (isNew) throw IllegalArgumentException()
        viewModelScope.launch {
            val result = repository.deleteEnvelope(form.envelope.id)
            releaseEnvelopeForm {
                it.copy(
                    state = when (result) {
                        Deleted -> FormState.Completed
                        NotFound -> FormState.Completed
                        Constraint -> FormState.Active(null) // TODO: 错误处理方式
                    },
                )
            }
        }
    }

    fun dismissEnvelopeFormError() {
        requestEnvelopeForm() ?: return
        releaseEnvelopeForm { it.copy(state = FormState.Active(null)) }
    }

    fun openDetailWithExistingBudget(id: BudgetId, date: LocalDate?) = detailModalState.update { state ->
        if (state != null && state.budgetForm.state == FormState.Busy) return@update state
        val budget = budgets.value.find { it.id == id } ?: return
        val allocations =
            if (date == null) allocations.value.filter { it.expense.happenedDate in budget.dateSpan }
            else allocations.value.filter { it.expense.happenedDate == date }
        DetailModalState(
            budgetForm = BudgetForm(
                state = FormState.Active(null),
                budget = budget,
                isNew = false,
                isDirty = false,
            ),
            allocations = allocations,
            dismissed = false,
        )
    }

    fun openDetailCreatingBudget(date: LocalDate) = detailModalState.update { state ->
        if (state != null && state.budgetForm.state == FormState.Busy) return@update state
        val allocations = allocations.value.filter { it.expense.happenedDate == date }
        DetailModalState(
            budgetForm = BudgetForm(
                state = FormState.Active(BudgetFormError.AmountNonPositive),
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

    fun updateBudget(budget: Budget) {
        detailModalState.update {
            if (it == null) return@update it
            if (it.budgetForm.budget.id != budget.id) return@update it
            it.copy(
                budgetForm = it.budgetForm.copy(
                    state = FormState.Active(
                        when {
                            budget.amount <= 0 -> BudgetFormError.AmountNonPositive
                            budgets.value.any { b -> b.dateSpan overlaps budget.dateSpan && b.id != budget.id } ->
                                BudgetFormError.BudgetOverlaps

                            else -> null
                        },
                    ),
                    budget = budget,
                    isDirty = true,
                ),
            )
        }
    }

    private fun requestBudgetForm(): BudgetForm? {
        val form = detailModalState.value?.budgetForm ?: return null
        var active = false
        detailModalState.update {
            if (it == null || it.dismissed ||
                it.budgetForm.state !is FormState.Active ||
                it.budgetForm.state.error != null
            ) return@update it
            active = true
            it.copy(budgetForm = it.budgetForm.copy(state = FormState.Busy))
        }
        if (!active) return null
        return form
    }

    private fun releaseBudgetForm(function: (DetailModalState) -> DetailModalState) {
        detailModalState.update {
            if (it == null || it.dismissed ||
                it.budgetForm.state !is FormState.Busy
            ) return@update it
            function(it)
        }
    }

    fun saveBudget() {
        val form = requestBudgetForm() ?: return

        if (isNew) {
            memoryBudgets.update { budgets ->
                if (form.isNew) budgets + form.budget
                else budgets.map { if (it.id == form.budget.id) form.budget else it }
            }
            releaseBudgetForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        } else viewModelScope.launch {
            val result =
                if (form.isNew) repository.insertBudget(form.budget)
                else repository.updateBudget(form.budget)
            releaseBudgetForm {
                it.copy(
                    budgetForm = it.budgetForm.copy(
                        state = when (result) {
                            EnvelopeArchived -> FormState.Active(BudgetFormError.EnvelopeArchived)
                            BudgetOverlaps -> FormState.Active(BudgetFormError.BudgetOverlaps)
                            Success -> FormState.Completed
                        },
                    ),
                )
            }
        }
    }

    fun deleteBudget() {
        val form = requestBudgetForm() ?: return
        if (isNew) {
            memoryBudgets.update { budgets ->
                if (form.isNew) budgets
                else budgets.filter { it.id != form.budget.id }
            }
            releaseBudgetForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        } else viewModelScope.launch {
            repository.deleteBudget(form.budget.id)
            releaseBudgetForm { it.copy(budgetForm = it.budgetForm.copy(state = FormState.Completed)) }
        }
    }

    fun dismissDetail() {
        detailModalState.update {
            if (it == null) return@update it
            it.copy(dismissed = true)
        }
    }

    fun clearDetail() {
        detailModalState.value = null
    }

    fun onEvent(event: EnvelopeEvent) = when (event) {
        is SelectMonth -> selectMonth(event.yearMonth)

        is InitializeNewEnvelope -> initializeNewEnvelope(event.name)
        is UpdateEnvelope -> updateEnvelope(event.envelope)
        SaveEnvelope -> saveEnvelope()
        ArchiveEnvelope -> archiveEnvelope()
        DeleteEnvelope -> deleteEnvelope()
        DismissEnvelopeFormError -> dismissEnvelopeFormError()

        is OpenDetailWithExistingBudget -> openDetailWithExistingBudget(event.id, event.date)
        is OpenDetailCreatingBudget -> openDetailCreatingBudget(event.date)
        is UpdateBudget -> updateBudget(event.budget)
        SaveBudget -> saveBudget()
        DeleteBudget -> deleteBudget()
        DismissDetail -> dismissDetail()
        ClearDetail -> clearDetail()
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeViewModel
    }
}
