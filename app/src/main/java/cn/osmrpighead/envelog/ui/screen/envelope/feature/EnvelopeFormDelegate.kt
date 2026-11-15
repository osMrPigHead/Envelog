package cn.osmrpighead.envelog.ui.screen.envelope.feature

import cn.osmrpighead.envelog.core.navigation.NavigationTarget
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.repository.BudgetWriteResult
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelopeDeleteResult
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeField
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeForm
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopeSubmissionError
import cn.osmrpighead.envelog.util.updateNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EnvelopeFormDelegate(
    private val target: NavigationTarget<EnvelopeId>,
    /** 新建信封时须与草稿预算/QuickSet 使用同一 id，由 ViewModel 统一生成注入 */
    private val envelopeId: EnvelopeId,
    private val budgets: StateFlow<List<Budget>>,     // insertEnvelope 携带草稿预算
    private val detailModal: DetailModalDelegate,     // save 前 awaitIdle
    private val quickSet: QuickSetDelegate,           // save 前 awaitIdle
    private val repository: EnvelogRepository,
    private val scope: CoroutineScope,
) {
    private val isNew = target is NavigationTarget.New

    private val _state = MutableStateFlow<EnvelopeForm?>(null)
    val state: StateFlow<EnvelopeForm?> = _state.asStateFlow()

    private val formStateOf: (EnvelopeForm) -> FormState<*>? = { it.state }

    /** 已有信封：从仓库加载 */
    fun loadExisting() {
        val existing = target as? NavigationTarget.Existing ?: return
        scope.launch {
            _state.value = EnvelopeForm(
                state = FormState.Active(null),
                envelope = repository.getEnvelope(existing.value),
                isNew = false,
                isDirty = false,
            )
        }
    }

    fun initializeNew(name: String) {
        _state.value = EnvelopeForm(
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
    fun update(field: EnvelopeField) = _state.updateNotNull {
        val envelope = when (field) {
            is EnvelopeField.Name -> it.envelope.copy(name = field.name)
            is EnvelopeField.IsBudgeted -> it.envelope.copy(isBudgeted = field.isBudgeted)
        }
        it.copy(envelope = envelope, isDirty = true)
    }

    private fun requestForm(): EnvelopeForm? = _state.requestBusy(
        allowError = true,
        formStateOf = formStateOf,
        toBusy = { it.copy(state = FormState.Busy) },
    )

    private fun releaseForm(transform: (EnvelopeForm) -> EnvelopeForm) =
        _state.releaseBusy(formStateOf, transform)

    fun save() {
        val form = requestForm() ?: return

        scope.launch {
            detailModal.awaitIdle()
            quickSet.awaitIdle()
            if (!isNew) {
                repository.updateEnvelope(form.envelope)
                releaseForm { it.copy(state = FormState.Completed) }
                return@launch
            }
            val result = repository.insertEnvelope(form.envelope, budgets.value)
            releaseForm {
                it.copy(
                    state = when (result) {
                        BudgetWriteResult.EnvelopeArchived -> throw IllegalArgumentException()
                        BudgetWriteResult.BudgetOverlaps -> FormState.Active(EnvelopeSubmissionError.BudgetOverlaps)
                        BudgetWriteResult.Success -> FormState.Completed
                    },
                )
            }
        }
    }

    fun archive() {
        val form = requestForm() ?: return
        if (isNew) throw IllegalArgumentException()
        scope.launch {
            repository.archiveEnvelope(form.envelope.id)
            releaseForm { it.copy(state = FormState.Completed) }
        }
    }

    fun delete() {
        val form = requestForm() ?: return
        if (isNew) throw IllegalArgumentException()
        scope.launch {
            val result = repository.deleteEnvelope(form.envelope.id)
            releaseForm {
                it.copy(
                    state = when (result) {
                        EnvelopeDeleteResult.Deleted -> FormState.Completed
                        EnvelopeDeleteResult.NotFound -> FormState.Completed
                        EnvelopeDeleteResult.Constraint -> FormState.Active(null) // TODO: 错误处理方式
                    },
                )
            }
        }
    }

    fun dismissError() {
        requestForm() ?: return
        releaseForm { it.copy(state = FormState.Active(null)) }
    }
}
