package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import cn.osmrpighead.envelog.ui.screen.envelope.feature.BudgetDraftStore
import cn.osmrpighead.envelog.ui.screen.envelope.feature.DetailModalDelegate
import cn.osmrpighead.envelog.ui.screen.envelope.feature.EnvelopeFormDelegate
import cn.osmrpighead.envelog.ui.screen.envelope.feature.QuickSetDelegate
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.time.DayOfWeek
import java.time.YearMonth

@HiltViewModel(assistedFactory = EnvelopeViewModel.Factory::class)
class EnvelopeViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Envelope,
    private val appState: AppStateHolder,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = appState.yearMonth
    private val today = appState.today
    fun selectMonth(yearMonth: YearMonth) = appState.selectMonth(yearMonth)

    private val isNew = route.id is NavigationTarget.New
    private val envelopeId = if (route.id is NavigationTarget.Existing) route.id.value else EnvelopeId.generate()

    private val draftStore = BudgetDraftStore()

    private val isDeletable =
        if (route.id is NavigationTarget.Existing) repository.observeEnvelopeDeletable(route.id.value)
        else flowOf(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val budgets: StateFlow<List<Budget>> = if (route.id is NavigationTarget.Existing)
        yearMonth.flatMapLatest { yearMonth ->
            repository.observeBudgets(
                envelopeId = route.id.value,
                dateSpan = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    else draftStore.state

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allocations: StateFlow<List<AllocationWithInfo>> = if (route.id is NavigationTarget.Existing)
        yearMonth.flatMapLatest { yearMonth ->
            repository.observeAllocations(
                envelopeId = route.id.value,
                dateSpan = yearMonth.toHalfOpenDateSpan(), // TODO: User rule
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    else MutableStateFlow(emptyList())

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

    private val detailModal = DetailModalDelegate(
        isNew = isNew,
        envelopeId = envelopeId,
        yearMonth = yearMonth,
        budgets = budgets,
        allocations = allocations,
        draftStore = draftStore,
        repository = repository,
        scope = viewModelScope,
    )
    private val quickSet = QuickSetDelegate(
        isNew = isNew,
        envelopeId = envelopeId,
        yearMonth = yearMonth,
        draftStore = draftStore,
        repository = repository,
        scope = viewModelScope,
    )
    private val envelopeForm = EnvelopeFormDelegate(
        target = route.id,
        envelopeId = envelopeId,
        budgets = budgets,
        detailModal = detailModal,
        quickSet = quickSet,
        repository = repository,
        scope = viewModelScope,
    )

    private val detailNotReady = when (route.id) {
        is NavigationTarget.Existing -> EnvelopeDetail.Loading
        is NavigationTarget.New -> EnvelopeDetail.IsNew
    }
    private val detail = combine(
        ledgerState,
        envelopeForm.state,
        detailModal.state,
        quickSet.state,
        isDeletable,
    ) { ledgerState, envelopeForm, detailModalState, quickSetFormModalState, isDeletable ->
        if (envelopeForm == null) detailNotReady
        else EnvelopeDetail.Success(ledgerState, envelopeForm, detailModalState, quickSetFormModalState, isDeletable)
    }
    val uiState =
        combine(yearMonth, today, detail) { yearMonth, today, detail -> EnvelopeUiState(yearMonth, today, detail) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EnvelopeUiState(yearMonth.value, today.value, detailNotReady),
            )

    init {
        envelopeForm.loadExisting()
    }

    fun onEvent(event: EnvelopeEvent) = when (event) {
        is SelectMonth -> selectMonth(event.yearMonth)

        is InitializeNewEnvelope -> envelopeForm.initializeNew(event.name)
        is UpdateEnvelope -> envelopeForm.update(event.envelope)
        SaveEnvelope -> envelopeForm.save()
        ArchiveEnvelope -> envelopeForm.archive()
        DeleteEnvelope -> envelopeForm.delete()
        DismissEnvelopeFormError -> envelopeForm.dismissError()

        is OpenDetailWithExistingBudget -> detailModal.openWithExistingBudget(event.id, event.date)
        is OpenDetailCreatingBudget -> detailModal.openCreatingBudget(event.date)
        is UpdateBudget -> detailModal.update(event.budget)
        SaveBudget -> detailModal.save()
        DeleteBudget -> detailModal.delete()
        DismissDetail -> detailModal.dismiss()
        ClearDetail -> detailModal.clear()

        OpenQuickSet -> quickSet.open()
        is UpdateQuickSet -> quickSet.update(event.field)
        is ApplyQuickSet -> quickSet.apply(event.overwrite)
        CancelOverwrite -> quickSet.cancelOverwrite()
        DismissQuickSet -> quickSet.dismiss()
        ClearQuickSet -> quickSet.clear()
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeViewModel
    }
}
