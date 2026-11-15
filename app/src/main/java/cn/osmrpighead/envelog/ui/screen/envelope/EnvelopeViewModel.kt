package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
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
import cn.osmrpighead.envelog.ui.screen.envelope.model.*
import cn.osmrpighead.envelog.util.updateNotNull
import cn.osmrpighead.envelog.util.withElementIf
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
    private val today = appState.today
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
    val uiState =
        combine(yearMonth, today, detail) { yearMonth, today, detail -> EnvelopeUiState(yearMonth, today, detail) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = EnvelopeUiState(yearMonth.value, today.value, detailNotReady),
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
        it.copy(envelope = envelope, isDirty = true)
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
            quickSetFormModalState.first { it?.state != FormState.Busy }
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

    fun openDetailCreatingBudget(date: LocalDate) = detailModalState.update { state ->
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

    fun updateBudget(budget: Budget) {
        detailModalState.update {
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
                            BudgetOverlaps -> FormState.Active(null)
                            Success -> FormState.Completed
                        },
                        fieldErrors = it.budgetForm.fieldErrors.withElementIf(
                            BudgetFieldError.BudgetOverlaps,
                            result == BudgetOverlaps,
                        ),
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

    fun openQuickSet() = quickSetFormModalState.update { state ->
        if (state != null && state.state == FormState.Busy) return@update state
        QuickSetFormModalState(
            state = FormState.Active(null),
            fieldErrors = setOf(QuickSetFieldError.AmountNonPositive),
            config = QuickSetFormState(
                mode = QuickSetMode.Daily,
                dailyConfig = QuickSetConfig.Daily(1, 0),
                weeklyConfig = QuickSetConfig.Weekly(DayOfWeek.MONDAY, DayOfWeek.SUNDAY, 0),
                monthlyConfig = QuickSetConfig.Monthly(0),
                targetSpan = yearMonth.value.toHalfOpenDateSpan().toClosed(),
            ),
            schedule = null,
            pendingOverwrite = null,
            dismissed = false,
        )
    }

    private fun Set<QuickSetFieldError>.checkAmount(config: QuickSetFormState) = withElementIf(
        QuickSetFieldError.AmountNonPositive,
        config.selectedConfig.amount <= 0,
    )

    private fun QuickSetFormModalState.updateWithAmountChecked(config: QuickSetFormState) = copy(
        fieldErrors = fieldErrors.checkAmount(config),
        config = config,
    ).reschedule()

    private fun Set<QuickSetFieldError>.checkInterval(config: QuickSetFormState) = withElementIf(
        QuickSetFieldError.IntervalInvalid,
        config.dailyConfig.intervalDays < 1 || config.dailyConfig.intervalDays > config.targetSpan.length,
    )

    private fun QuickSetFormModalState.reschedule(): QuickSetFormModalState {
        val schedule = if (fieldErrors.isEmpty()) QuickSetBudgetPlanner.schedule(
            config.selectedConfig,
            config.targetSpan,
            yearMonth.value,
        ) else null
        return copy(
            state = FormState.Active(if (schedule == null) QuickSetFormError.CannotGenerate else null),
            schedule = schedule,
        )
    }

    fun updateQuickSet(field: QuickSetField) = quickSetFormModalState.update {
        if (it == null || it.dismissed || it.state !is FormState.Active) return@update it
        when (field) {
            is QuickSetField.Mode -> {
                val config = it.config.copy(mode = field.mode)
                it.copy(fieldErrors = it.fieldErrors.checkInterval(config).checkAmount(config), config = config)
                    .reschedule()
            }

            is QuickSetField.TargetSpan -> {
                it.copy(config = it.config.copy(targetSpan = field.span)).reschedule()
            }

            is QuickSetField.Daily.Interval -> {
                val config = it.config.copy(dailyConfig = it.config.dailyConfig.copy(intervalDays = field.days))
                it.copy(fieldErrors = it.fieldErrors.checkInterval(config), config = config).reschedule()
            }

            is QuickSetField.Weekly.DaySpan -> {
                it.copy(
                    config = it.config.copy(
                        weeklyConfig = it.config.weeklyConfig.copy(
                            startDayOfWeek = field.start,
                            endDayOfWeek = field.end,
                        ),
                    ),
                ).reschedule()
            }

            is QuickSetField.Daily.Amount -> {
                it.updateWithAmountChecked(it.config.copy(dailyConfig = it.config.dailyConfig.copy(amount = field.amount)))
            }

            is QuickSetField.Weekly.Amount -> {
                it.updateWithAmountChecked(it.config.copy(weeklyConfig = it.config.weeklyConfig.copy(amount = field.amount)))
            }

            is QuickSetField.Monthly.Amount -> {
                it.updateWithAmountChecked(it.config.copy(monthlyConfig = it.config.monthlyConfig.copy(amount = field.amount)))
            }
        }
    }

    private fun requestQuickSetForm(): QuickSetFormModalState? {
        val form = quickSetFormModalState.value ?: return null
        var active = false
        quickSetFormModalState.update {
            if (it == null || it.dismissed || it.state !is FormState.Active || it.state.error != null) return@update it
            active = true
            it.copy(state = FormState.Busy)
        }
        if (!active) return null
        return form
    }

    private fun releaseQuickSetForm(function: (QuickSetFormModalState) -> QuickSetFormModalState) {
        quickSetFormModalState.update {
            if (it == null || it.dismissed || it.state !is FormState.Busy) return@update it
            function(it)
        }
    }

    fun cancelQuickSetOverwrite() {
        quickSetFormModalState.update {
            if (it == null || it.dismissed || it.pendingOverwrite == null) return@update it
            it.copy(
                state = FormState.Active(if (it.schedule == null) QuickSetFormError.CannotGenerate else null),
                pendingOverwrite = null,
            )
        }
    }

    fun dismissQuickSet() {
        quickSetFormModalState.update {
            if (it == null) return@update it
            it.copy(dismissed = true)
        }
    }

    fun clearQuickSet() {
        quickSetFormModalState.value = null
    }

    fun applyQuickSet(overwrite: Boolean) {
        val form = requestQuickSetForm() ?: return
        viewModelScope.launch {
            val config = form.config
            val selected = config.selectedConfig
            val schedule = QuickSetBudgetPlanner.schedule(selected, config.targetSpan, yearMonth.value)
            if (schedule == null) {
                releaseQuickSetForm { it.copy(state = FormState.Active(QuickSetFormError.CannotGenerate)) }
                return@launch
            }

            val plan = QuickSetBudgetPlanner.plan(
                config = selected,
                selection = config.targetSpan,
                yearMonth = yearMonth.value,
                envelopeId = envelopeId,
                existing = readExisting(schedule.coveredSpan()),
            )
            if (plan == null) {
                releaseQuickSetForm { it.copy(state = FormState.Active(QuickSetFormError.CannotGenerate)) }
            } else if (plan.conflicts.isNotEmpty() && !overwrite) {
                releaseQuickSetForm {
                    it.copy(state = FormState.Active(null), pendingOverwrite = plan.conflicts)
                }
            } else persistQuickSet(plan)
        }
    }

    private suspend fun persistQuickSet(plan: QuickSetBudgetPlan) {
        val conflictIds = plan.conflicts.map { it.id }

        if (isNew) {
            memoryBudgets.update { budgets -> budgets.filterNot { it.id in conflictIds } + plan.budgets }
            releaseQuickSetForm { it.copy(state = FormState.Completed, pendingOverwrite = null) }
        } else {
            val result = repository.replaceBudgets(
                envelopeId = envelopeId,
                deleteBudgetIds = conflictIds,
                budgets = plan.budgets,
            )
            releaseQuickSetForm {
                it.copy(
                    state = when (result) {
                        EnvelopeArchived -> FormState.Active(QuickSetFormError.EnvelopeArchived)
                        BudgetOverlaps -> FormState.Active(QuickSetFormError.CannotGenerate)
                        Success -> FormState.Completed
                    },
                    pendingOverwrite = null,
                )
            }
        }
    }

    private suspend fun readExisting(coveredSpan: HalfOpenDateSpan): List<Budget> =
        if (isNew) memoryBudgets.value
        else repository.observeBudgets(envelopeId = envelopeId, dateSpan = coveredSpan).first()

    /** 末段预算按完整周期产出、不裁剪到显示月，冲突检测须按其实际覆盖的包络重读。 */
    private fun QuickSetSchedule.coveredSpan(): HalfOpenDateSpan = HalfOpenDateSpan(
        startDate = segments.minOf { it.span.startDate },
        endDate = segments.maxOf { it.span.endDate },
    )

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

        OpenQuickSet -> openQuickSet()
        is UpdateQuickSet -> updateQuickSet(event.field)
        is ApplyQuickSet -> applyQuickSet(event.overwrite)
        CancelOverwrite -> cancelQuickSetOverwrite()
        DismissQuickSet -> dismissQuickSet()
        ClearQuickSet -> clearQuickSet()
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Envelope): EnvelopeViewModel
    }
}
