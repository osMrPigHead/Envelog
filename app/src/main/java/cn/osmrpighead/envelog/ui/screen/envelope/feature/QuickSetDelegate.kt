package cn.osmrpighead.envelog.ui.screen.envelope.feature

import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetField
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFieldError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormError
import cn.osmrpighead.envelog.ui.screen.envelope.QuickSetFormModalState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlan
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetBudgetPlanner
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetFormState
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetMode
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetSchedule
import cn.osmrpighead.envelog.util.withElementIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.YearMonth

class QuickSetDelegate(
    private val isNew: Boolean,
    private val envelopeId: EnvelopeId,
    private val yearMonth: StateFlow<YearMonth>,
    /** 新建信封草稿预算仓库；isNew 时 readExisting/persist 走它 */
    private val draftStore: BudgetDraftStore,
    private val repository: EnvelogRepository,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<QuickSetFormModalState?>(null)
    val state: StateFlow<QuickSetFormModalState?> = _state.asStateFlow()

    private val formStateOf: (QuickSetFormModalState) -> FormState<*>? =
        { if (it.dismissed) null else it.state }

    /** 等待表单离开 Busy 态，供 EnvelopeFormDelegate 保存前协调使用 */
    suspend fun awaitIdle() {
        _state.first { it?.state != FormState.Busy }
    }

    fun open() = _state.update { state ->
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

    fun update(field: QuickSetField) = _state.update {
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

    private fun requestForm(): QuickSetFormModalState? = _state.requestBusy(
        allowError = false,
        formStateOf = formStateOf,
        toBusy = { it.copy(state = FormState.Busy) },
    )

    private fun releaseForm(transform: (QuickSetFormModalState) -> QuickSetFormModalState) =
        _state.releaseBusy(formStateOf, transform)

    fun cancelOverwrite() {
        _state.update {
            if (it == null || it.dismissed || it.pendingOverwrite == null) return@update it
            it.copy(
                state = FormState.Active(if (it.schedule == null) QuickSetFormError.CannotGenerate else null),
                pendingOverwrite = null,
            )
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

    fun apply(overwrite: Boolean) {
        val form = requestForm() ?: return
        scope.launch {
            val config = form.config
            val selected = config.selectedConfig
            val schedule = QuickSetBudgetPlanner.schedule(selected, config.targetSpan, yearMonth.value)
            if (schedule == null) {
                releaseForm { it.copy(state = FormState.Active(QuickSetFormError.CannotGenerate)) }
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
                releaseForm { it.copy(state = FormState.Active(QuickSetFormError.CannotGenerate)) }
            } else if (plan.conflicts.isNotEmpty() && !overwrite) {
                releaseForm {
                    it.copy(state = FormState.Active(null), pendingOverwrite = plan.conflicts)
                }
            } else persist(plan)
        }
    }

    private suspend fun persist(plan: QuickSetBudgetPlan) {
        val conflictIds = plan.conflicts.map { it.id }

        if (isNew) {
            draftStore.update { budgets -> budgets.filterNot { it.id in conflictIds } + plan.budgets }
            releaseForm { it.copy(state = FormState.Completed, pendingOverwrite = null) }
        } else {
            val result = repository.replaceBudgets(
                envelopeId = envelopeId,
                deleteBudgetIds = conflictIds,
                budgets = plan.budgets,
            )
            releaseForm {
                it.copy(
                    state = when (result) {
                        BudgetWriteResult.EnvelopeArchived -> FormState.Active(QuickSetFormError.EnvelopeArchived)
                        BudgetWriteResult.BudgetOverlaps -> FormState.Active(QuickSetFormError.CannotGenerate)
                        BudgetWriteResult.Success -> FormState.Completed
                    },
                    pendingOverwrite = null,
                )
            }
        }
    }

    private suspend fun readExisting(coveredSpan: HalfOpenDateSpan): List<Budget> =
        if (isNew) draftStore.current()
        else repository.observeBudgets(envelopeId = envelopeId, dateSpan = coveredSpan).first()

    /** 末段预算按完整周期产出、不裁剪到显示月，冲突检测须按其实际覆盖的包络重读。 */
    private fun QuickSetSchedule.coveredSpan(): HalfOpenDateSpan = HalfOpenDateSpan(
        startDate = segments.minOf { it.span.startDate },
        endDate = segments.maxOf { it.span.endDate },
    )
}
