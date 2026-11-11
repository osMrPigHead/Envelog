package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.hasOverlap
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult.*
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlanner
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@HiltViewModel(assistedFactory = EnvelopeViewModel.Factory::class)
class EnvelopeViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Envelope,
    @Assisted private val back: () -> Unit,
    private val appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    fun selectMonth(yearMonth: YearMonth) {
        appState.selectMonth(yearMonth)
        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        if (!uiState.isNew) fetchEnvelope()
        else _uiState.update {
            if (it !is EnvelopeUiState.Success) return@update it
            it.copy(
                yearMonth = yearMonth,
                activeItem = ActiveItem(
                    budget = null,
                    isNew = false,
                    error = null,
                    complete = true,
                ),
            )
        }
    }

    private val _uiState = MutableStateFlow(
        if (route.id is NavigationTarget.New) EnvelopeUiState.IsNew else EnvelopeUiState.Loading,
    )
    val uiState = _uiState.asStateFlow()
    private val fieldId = Any()
    private var job: Job? = null

    private val savedEnvelope = flow {
        if (route.id is NavigationTarget.Existing) emit(repository.getEnvelope(route.id.value))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val savedBudgets = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeBudgets(
            envelopeId = route.id.value,
            dateSpan = yearMonth.toHalfOpenDateSpan(),  // TODO: User rule
        ).map { budgets -> budgets.associateBy { it.id } }
    } else flowOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allocations = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeDailyAllocationOverview(
            envelopeId = route.id.value,
            dateSpan = yearMonth.toHalfOpenDateSpan(),
        )
    } else flowOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val monthlyAllocations: Flow<List<AllocationWithInfo>> =
        if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
            repository.observeAllocations(route.id.value, yearMonth.toHalfOpenDateSpan())
                .map { allocations -> allocations.sortedBy { it.expenditure.happenedAt } }
        } else flowOf(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeAllocations: Flow<List<AllocationWithInfo>> =
        if (route.id is NavigationTarget.Existing) _uiState
            .map { (it as? EnvelopeUiState.Success)?.activeItem?.budget?.dateSpan }
            .distinctUntilChanged()
            .flatMapLatest { span ->
                if (span == null) flowOf(emptyList())
                else repository.observeAllocations(route.id.value, span)
                    .map { allocations -> allocations.sortedBy { it.expenditure.happenedAt } }
            }
        else flowOf(emptyList())

    fun initializeForNew(envelope: Envelope) {
        _uiState.value = EnvelopeUiState.Success(
            envelope = envelope,
            isNew = true,
            yearMonth = yearMonth.value,
            today = appState.today.value,
            budgets = emptyMap(),
            amountFieldId = fieldId,
            allocations = emptyList(),
            allocationsWithInfo = emptyList(),
            activeItem = ActiveItem(
                budget = null,
                isNew = false,
                error = null,
                complete = true,
            ),
            envelopeSubmissionError = null,
        )
    }

    fun fetchEnvelope() {
        viewModelScope.launch {
            val success = combine(
                savedEnvelope,
                savedBudgets,
                allocations,
                monthlyAllocations,
            ) { envelope, budgets, allocations, allocationsWithInfo ->
                EnvelopeUiState.Success(
                    envelope = envelope,
                    isNew = false,
                    yearMonth = yearMonth.value,
                    today = appState.today.value,
                    budgets = budgets,
                    amountFieldId = fieldId,
                    allocations = allocations,
                    allocationsWithInfo = allocationsWithInfo,
                    activeItem = ActiveItem(
                        budget = null,
                        isNew = false,
                        error = null,
                        complete = true,
                    ),
                    envelopeSubmissionError = null,
                )
            }.first()
            _uiState.value = success
        }
    }

    init {
        if (route.id is NavigationTarget.Existing) {
            fetchEnvelope()
            viewModelScope.launch {
                activeAllocations.collect { allocations ->
                    _uiState.update {
                        if (it !is EnvelopeUiState.Success) return@update it
                        it.copy(
                            activeItem = it.activeItem.copy(allocations = allocations),
                        )
                    }
                }
            }
        }
    }

    fun updateEnvelope(envelope: Envelope) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(envelope = envelope)
        }
    }

    fun openActiveBudget(id: BudgetId) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeItem = ActiveItem(
                    budget = it.budgets[id],
                    isNew = false,
                    error = null,
                    complete = false,
                ),
            )
        }
    }

    fun newActiveBudget(date: LocalDate) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeItem = ActiveItem(
                    budget = Budget(
                        id = BudgetId.generate(),
                        envelopeId = it.envelope.id,
                        dateSpan = date untilExclusive date.plusDays(1),
                        attributedMonth = yearMonth.value,
                        amount = 0,
                    ),
                    isNew = true,
                    error = ActiveBudgetError.AmountNonPositive,
                    complete = false,
                ),
            )
        }
    }

    fun updateActiveBudget(budget: Budget) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            if (it.activeItem.budget?.id != budget.id) return@update it
            it.copy(
                activeItem = it.activeItem.copy(
                    budget = budget,
                    error =
                        when {
                            budget.amount <= 0 -> ActiveBudgetError.AmountNonPositive
                            (it.budgets + (budget.id to budget)).map { budget -> budget.value.dateSpan }.hasOverlap() ->
                                ActiveBudgetError.BudgetOverlaps

                            else -> null
                        },
                ),
            )
        }
    }

    fun closeActiveBudget() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(activeItem = it.activeItem.copy(complete = true))
        }
    }

    fun saveActiveBudget() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        val budget = uiState.activeItem.budget ?: return

        if (budget.amount <= 0) {
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(activeItem = it.activeItem.copy(error = ActiveBudgetError.AmountNonPositive))
            }
            return
        }

        job = viewModelScope.launch {
            val result = if (!uiState.isNew) {
                if (uiState.activeItem.isNew) repository.insertBudget(budget)
                else repository.updateBudget(budget)
            } else null

            if (result != null && result != Success) {
                _uiState.update {
                    if (it !is EnvelopeUiState.Success) return@update it
                    it.copy(
                        activeItem = it.activeItem.copy(
                            error = when (result) {
                                EnvelopeArchived -> ActiveBudgetError.EnvelopeArchived
                                BudgetOverlaps -> ActiveBudgetError.BudgetOverlaps
                            },
                        ),
                    )
                }
                return@launch
            }
            val updatedBudgets = if (!uiState.isNew) savedBudgets.first() else null
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(
                    budgets = updatedBudgets ?: (it.budgets + (budget.id to budget)),
                    activeItem = it.activeItem.copy(complete = true),
                )
            }
        }
    }

    fun deleteActiveBudget() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        val budget = uiState.activeItem.budget ?: return
        job = viewModelScope.launch {
            if (!uiState.isNew && !uiState.activeItem.isNew) repository.deleteBudget(budget.id)
            val updatedBudgets = if (!uiState.isNew) savedBudgets.first() else null
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(
                    budgets = updatedBudgets ?: (it.budgets - budget.id),
                    activeItem = it.activeItem.copy(complete = true),
                )
            }
        }
    }

    fun clearActiveBudget() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeItem = ActiveItem(
                    budget = null,
                    isNew = false,
                    error = null,
                    complete = false,
                ),
            )
        }
    }

    fun openQuickSet() {
        _uiState.update {
            if (it !is EnvelopeUiState.Success) return@update it
            it.copy(quickSet = QuickSetUiState(open = true))
        }
    }

    fun dismissQuickSet() {
        _uiState.update {
            if (it !is EnvelopeUiState.Success || !it.quickSet.open) return@update it
            it.copy(
                quickSet = it.quickSet.copy(
                    complete = true,
                    conflict = null,
                    error = null,
                ),
            )
        }
    }

    fun clearQuickSet() {
        _uiState.update {
            if (it !is EnvelopeUiState.Success) return@update it
            it.copy(quickSet = QuickSetUiState())
        }
    }

    fun applyQuickSet(config: QuickSetConfig, targetSpan: ClosedDateSpan) {
        if (job?.isActive == true) return

        val state = _uiState.value as? EnvelopeUiState.Success ?: return
        if (config.amount <= 0) return

        val plan = QuickSetBudgetPlanner.plan(
            config = config,
            selection = targetSpan,
            yearMonth = state.yearMonth,
            envelopeId = state.envelope.id,
            existing = state.budgets.values,
        ) ?: return

        if (plan.conflicts.isNotEmpty()) {
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(
                    quickSet = it.quickSet.copy(
                        conflict = plan,
                        error = null,
                    ),
                )
            }
            return
        }

        _uiState.update {
            if (it !is EnvelopeUiState.Success) return@update it
            it.copy(quickSet = it.quickSet.copy(error = null))
        }
        persistQuickSet(plan)
    }

    fun confirmQuickSetOverwrite() {
        if (job?.isActive == true) return

        val state = _uiState.value as? EnvelopeUiState.Success ?: return
        val plan = state.quickSet.conflict ?: return
        persistQuickSet(plan)
    }

    fun cancelQuickSetOverwrite() {
        _uiState.update {
            if (it !is EnvelopeUiState.Success) return@update it
            it.copy(quickSet = it.quickSet.copy(conflict = null))
        }
    }

    private fun persistQuickSet(plan: QuickSetBudgetPlan) {
        job = viewModelScope.launch {
            if (!isNew) {
                val state = _uiState.value as? EnvelopeUiState.Success ?: return@launch
                val result = repository.replaceBudgets(
                    envelopeId = state.envelope.id,
                    deleteBudgetIds = plan.conflicts.map { it.id },
                    budgets = plan.budgets,
                )
                if (result != Success) {
                    _uiState.update {
                        if (it !is EnvelopeUiState.Success) return@update it
                        if (!it.quickSet.open || it.quickSet.complete) return@update it
                        it.copy(
                            quickSet = it.quickSet.copy(
                                conflict = null,
                                error = if (result == EnvelopeArchived) {
                                    QuickSetError.EnvelopeArchived
                                } else {
                                    QuickSetError.BudgetOverlaps
                                },
                            ),
                        )
                    }
                    return@launch
                }
            }

            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                val removed = plan.conflicts.map { conflict -> conflict.id }.toSet()
                it.copy(
                    budgets = (it.budgets - removed) + plan.budgets.associateBy { budget -> budget.id },
                    quickSet = if (it.quickSet.open) {
                        it.quickSet.copy(
                            complete = true,
                            conflict = null,
                            error = null,
                        )
                    } else {
                        it.quickSet
                    },
                )
            }
        }
    }

    fun saveAndBack() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        job = viewModelScope.launch {
            if (!uiState.isNew) {
                repository.updateEnvelope(uiState.envelope)
                back()
                return@launch
            }
            val result = repository.insertEnvelope(uiState.envelope, uiState.budgets.values.toList())
            when (result) {
                Success -> back()
                EnvelopeArchived -> back()
                BudgetOverlaps -> _uiState.update {
                    if (it !is EnvelopeUiState.Success) return@update it
                    it.copy(envelopeSubmissionError = EnvelopeSubmissionError.BudgetOverlaps)
                }
            }
        }
    }

    fun clearSubmissionError() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(envelopeSubmissionError = null)
        }
    }

    fun archiveAndBack() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        job = viewModelScope.launch {
            if (!uiState.isNew) repository.archiveEnvelope(uiState.envelope.id)
            back()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope, back: () -> Unit): EnvelopeViewModel
    }
}