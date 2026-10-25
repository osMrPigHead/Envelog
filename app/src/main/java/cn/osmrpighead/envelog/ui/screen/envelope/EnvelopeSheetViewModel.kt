package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.DateRangeRule
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.model.Field
import cn.osmrpighead.envelog.ui.model.MutableField
import cn.osmrpighead.envelog.navigation.AppRoute
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private data class MutableFields(
    val name: MutableField<String> = MutableField(),
    val isBudgeted: MutableField<Boolean> = MutableField(),
) {
    fun toFields(): EnvelopeSheetFields = EnvelopeSheetFields(name.toField(), isBudgeted.toField())
}

data class EnvelopeSheetFields(
    val name: Field<String> = Field(),
    val isBudgeted: Field<Boolean> = Field(),
)

@HiltViewModel(assistedFactory = EnvelopeSheetViewModel.Factory::class)
class EnvelopeSheetViewModel @AssistedInject constructor(
    @Assisted private val route: AppRoute.Envelope,
    appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth

    private var fields: MutableFields = MutableFields()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EnvelopeSheetUiState> = yearMonth.flatMapLatest { yearMonth ->
        val range = DateRange(
            start = yearMonth.atDay(1),
            end = yearMonth.plusMonths(1).atDay(1),
            rule = DateRangeRule.StartInRange,
        )
        try {
            fields = MutableFields()
            repository.observeEnvelopeWithExtra(
                envelopeId = route.id,
                range = range,
            ).map { result ->
                EnvelopeSheetUiState.Success(
                    value = result,
                    yearMonth = yearMonth,
                    calendarModel = BudgetCalendarModel(
                        allocations = result.allocations
                            .associate { it.transaction.dateTime.toLocalDate() to it.allocation.amount },
                        budgets = result.budgets,
                    ),
                    fields = fields.toFields(),
                    budgetItem = null,
                )
            }
        } catch (_: EnvelogRepositoryException.ArgumentException.NoSuch.Envelope) {
            flowOf(EnvelopeSheetUiState.NoSuchEnvelope)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EnvelopeSheetUiState.Loading,
    )

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeSheetViewModel
    }
}