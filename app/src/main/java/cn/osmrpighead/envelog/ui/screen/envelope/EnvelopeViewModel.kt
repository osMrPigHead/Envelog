package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

@HiltViewModel(assistedFactory = EnvelopeViewModel.Factory::class)
class EnvelopeViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Envelope,
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

    private val savedEnvelope = flow {
        if (route.id is NavigationTarget.Existing) emit(repository.getEnvelopeById(route.id.value))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val savedBudgets = if (route.id is NavigationTarget.Existing) yearMonth.flatMapLatest { yearMonth ->
        repository.observeBudgets(
            envelopeId = route.id.value,
            attributedMonth = yearMonth,
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
            activeBudget = null,
            activeIsNew = false,
            fieldId = fieldId,
            allocations = emptyList(),
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
                activeBudget = null,
                activeIsNew = false,
                fieldId = fieldId,
                allocations = allocations,
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
            it.copy(activeBudget = it.budgets[id], activeIsNew = false)
        }
    }

    fun newActiveBudget(date: LocalDate) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(
                activeBudget = Budget(
                    id = BudgetId.generate(),
                    envelopeId = it.envelope.id,
                    dateSpan = date untilExclusive date.plusDays(1),
                    attributedMonth = yearMonth.value,
                    amount = 0,
                ),
                activeIsNew = true,
            )
        }
    }

    fun updateActiveBudget(budget: Budget) {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(activeBudget = budget)
        }
    }

    fun closeActiveBudget() {
        _uiState.update {
            require(it is EnvelopeUiState.Success)
            it.copy(activeBudget = null)
        }
    }

    fun saveActiveBudget(callback: suspend () -> Unit) {
        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        val budget = uiState.activeBudget ?: return
        viewModelScope.launch {
            if (!isNew) {
                if (uiState.activeIsNew) repository.insertBudget(budget)
                else repository.updateBudget(budget)
            }
            try {
                callback()
            } finally {
                _uiState.update {
                    if (it !is EnvelopeUiState.Success) return@update it
                    it.copy(
                        budgets = it.budgets + (budget.id to budget),
                        activeBudget = null,
                    )
                }
            }
        }
    }

    fun save() {
        val uiState = _uiState.value
        require(uiState is EnvelopeUiState.Success)
        viewModelScope.launch {
            if (!isNew) {
                repository.updateEnvelope(uiState.envelope)
                return@launch
            }
            repository.insertEnvelope(uiState.envelope, uiState.budgets.values.toList())
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeViewModel
    }
}