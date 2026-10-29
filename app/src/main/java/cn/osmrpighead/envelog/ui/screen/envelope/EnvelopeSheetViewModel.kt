package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.DateRangeRule
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.navigation.AppRoute
import cn.osmrpighead.envelog.navigation.loadSuspend
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.model.FieldWithState
import cn.osmrpighead.envelog.ui.model.ModelTarget
import cn.osmrpighead.envelog.ui.model.SubmitState
import cn.osmrpighead.envelog.ui.model.modelTargetExistingOrNew
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeSheetModel
import cn.osmrpighead.envelog.util.date.DatePeriod
import cn.osmrpighead.envelog.util.date.first
import cn.osmrpighead.envelog.util.date.next
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class EnvelopeSheetFields(
    val name: FieldWithState<String> = FieldWithState(),
    val isBudgeted: FieldWithState<Boolean> = FieldWithState(),
)

@HiltViewModel(assistedFactory = EnvelopeSheetViewModel.Factory::class)
class EnvelopeSheetViewModel @AssistedInject constructor(
    @Assisted private val route: AppRoute.Envelope,
    appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    private val today = appState.today

    private val envelopeSheetFields = MutableStateFlow(EnvelopeSheetFields())
    private val budgetItemSheetModel = MutableStateFlow<BudgetItemSheetModel?>(null)

    private var budgets: List<Budget> = emptyList()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val sheetModel: Flow<ModelTarget<EnvelopeSheetModel>> = yearMonth.flatMapLatest { yearMonth ->
        val range = DateRange(
            start = yearMonth.first,
            end = yearMonth.next.first,
            rule = DateRangeRule.StartInRange,
        )
        route.id.loadSuspend(
            existing = { id ->
                repository.observeEnvelopeWithExtra(id, range).map { result ->
                    budgets = result.budgets
                    ModelTarget.Existing(
                        EnvelopeSheetModel(
                            envelope = result.envelope,
                            calendarModel = BudgetCalendarModel(
                                allocations = result.allocations
                                    .associate { it.transaction.dateTime.toLocalDate() to it.allocation.amount },
                                budgets = result.budgets,
                            ),
                        ),
                    )
                }
            },
            new = { flowOf(ModelTarget.New()) },
        )
    }

    val uiState: StateFlow<EnvelopeSheetUiState> = combine(
        sheetModel, envelopeSheetFields, budgetItemSheetModel,
    ) { sheetModel, envelopeSheetFields, budgetItemSheetModel ->
        EnvelopeSheetUiState(
            target = sheetModel,
            yearMonth = yearMonth.value,
            fields = envelopeSheetFields,
            budgetItemSheetModel = budgetItemSheetModel,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EnvelopeSheetUiState(
            target = ModelTarget.Loading(),
            yearMonth = yearMonth.value,
            fields = envelopeSheetFields.value,
            budgetItemSheetModel = budgetItemSheetModel.value,
        ),
    )

    fun onNameChange(name: String) = envelopeSheetFields.update {
        it.copy(name = FieldWithState(name, SubmitState.Idle))
    }

    fun onBudgetedChange(budgeted: Boolean) = envelopeSheetFields.update {
        it.copy(isBudgeted = FieldWithState(budgeted, SubmitState.Idle))
    }

    fun onDateClicked(date: LocalDate) = budgetItemSheetModel.update { _ ->
        BudgetItemSheetModel(
            target = modelTargetExistingOrNew(budgets.find { date in it.startDate..<it.endDate }),
            fields = BudgetItemSheetFields(),
            today = today.value,
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    fun confirmBudgetItemSheetChange(sheetState: SheetValue): Boolean {
        if (sheetState != SheetValue.Hidden) return true
        if (budgetItemSheetModel.value?.fields?.let {it.amount.value == null && it.period.value == null } ?: true)
            return true
        // TODO: Hint change not saved
        return false
    }

    fun onPeriodChange(period: DatePeriod) = budgetItemSheetModel.update {
        it?.copy(
            fields = it.fields.copy(period = FieldWithState(period, SubmitState.Idle)),
        )
    }

    fun onAmountChange(amount: Long) = budgetItemSheetModel.update {
        it?.copy(
            fields = it.fields.copy(amount = FieldWithState(amount, SubmitState.Idle)),
        )
    }

    fun onSaveBudgetItem() {

    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeSheetViewModel
    }
}