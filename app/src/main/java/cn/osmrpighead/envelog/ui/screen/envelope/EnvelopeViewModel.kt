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
    private var activeBudgetIsNew = false
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

    fun initializeForNew(envelope: Envelope) {
        _uiState.value = EnvelopeUiState.Success(
            envelope = envelope,
            yearMonth = yearMonth.value,
            budgets = emptyMap(),
            amountFieldId = fieldId,
            allocations = emptyList(),
            activeBudget = ActiveBudget(
                budget = null,
                error = null,
                complete = true,
            ),
            envelopeSubmissionError = null,
        )
    }

    init {
        if (route.id is NavigationTarget.Existing) viewModelScope.launch {
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
                activeBudget = ActiveBudget(
                    budget = null,
                    error = null,
                    complete = true,
                ),
                envelopeSubmissionError = null,
            )
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
            activeBudgetIsNew = false
            it.copy(
                activeBudget = ActiveBudget(
                    budget = it.budgets[id],
                    error = null,
                    complete = false,
                ),
            )
        }
    }

    fun newActiveBudget(date: LocalDate) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            activeBudgetIsNew = true
            it.copy(
                activeBudget = ActiveBudget(
                    budget = Budget(
                        id = BudgetId.generate(),
                        envelopeId = it.envelope.id,
                        dateSpan = date untilExclusive date.plusDays(1),
                        attributedMonth = yearMonth.value,
                        amount = 0,
                    ),
                    error = ActiveError.AmountNonPositive,
                    complete = false,
                ),
            )
        }
    }

    fun updateActiveBudget(budget: Budget) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeBudget = it.activeBudget.copy(
                    budget = budget,
                    error =
                        when {
                            budget.amount <= 0 -> ActiveError.AmountNonPositive
                            (it.budgets + (budget.id to budget)).map { budget -> budget.value.dateSpan }.hasOverlap() ->
                                ActiveError.BudgetOverlaps

                            else -> null
                        },
                ),
            )
        }
    }

    fun closeActiveBudget() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(activeBudget = it.activeBudget.copy(complete = true))
        }
    }

    fun saveActiveBudget() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        val budget = uiState.activeBudget.budget ?: return

        if (budget.amount <= 0) {
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(activeBudget = it.activeBudget.copy(error = ActiveError.AmountNonPositive))
            }
            return
        }

        job = viewModelScope.launch {
            val result = if (!isNew) {
                if (activeBudgetIsNew) repository.insertBudget(budget)
                else repository.updateBudget(budget)
            } else null

            if (result != null && result != Success) {
                _uiState.update {
                    if (it !is EnvelopeUiState.Success) return@update it
                    it.copy(
                        activeBudget = it.activeBudget.copy(
                            error = when (result) {
                                EnvelopeArchived -> ActiveError.EnvelopeArchived
                                BudgetOverlaps -> ActiveError.BudgetOverlaps
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
                    activeBudget = it.activeBudget.copy(complete = true),
                )
            }
        }
    }

    fun deleteActiveBudget() {
        if (job?.isActive == true) return

        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        val budget = uiState.activeBudget.budget ?: return
        job = viewModelScope.launch {
            if (!isNew && !activeBudgetIsNew) repository.deleteBudget(budget.id)
            val updatedBudgets = if (!isNew) savedBudgets.first() else null
            _uiState.update {
                if (it !is EnvelopeUiState.Success) return@update it
                it.copy(
                    budgets = updatedBudgets ?: (it.budgets - budget.id),
                    activeBudget = it.activeBudget.copy(complete = true),
                )
            }
        }
    }

    fun clearActiveBudget() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeBudget = ActiveBudget(
                    budget = null,
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