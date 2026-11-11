package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
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
    }

    private val isNew = route.id is NavigationTarget.New
    private val _uiState = MutableStateFlow(if (isNew) EnvelopeUiState.IsNew else EnvelopeUiState.Loading)
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
            yearMonth = yearMonth.value,
            budgets = emptyMap(),
            amountFieldId = fieldId,
            allocations = emptyList(),
            activeItem = ActiveItem(
                budget = null,
                isNew = false,
                error = null,
                complete = true,
            ),
            envelopeSubmissionError = null,
        )
    }

    init {
        if (route.id is NavigationTarget.Existing) {
            viewModelScope.launch {
                val (envelope, budgets, allocations) =
                    combine(savedEnvelope, savedBudgets, allocations) { envelope, budgets, allocations ->
                        Triple(envelope, budgets, allocations)
                    }.first()
                _uiState.value = EnvelopeUiState.Success(
                    envelope = envelope,
                    yearMonth = yearMonth.value,
                    budgets = budgets,
                    amountFieldId = fieldId,
                    allocations = allocations,
                    activeItem = ActiveItem(
                        budget = null,
                        isNew = false,
                        error = null,
                        complete = true,
                    ),
                    envelopeSubmissionError = null,
                )
            }
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
            val result = if (!isNew) {
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
            val updatedBudgets = if (!isNew) savedBudgets.first() else null
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
            if (!isNew && !uiState.activeItem.isNew) repository.deleteBudget(budget.id)
            val updatedBudgets = if (!isNew) savedBudgets.first() else null
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

    fun saveAndBack() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        job = viewModelScope.launch {
            if (!isNew) {
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

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope, back: () -> Unit): EnvelopeViewModel
    }
}