package cn.osmrpighead.envelog.ui.screen.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.data.model.ExpenseAllocation
import cn.osmrpighead.envelog.data.model.ExpenseId
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Clock

@HiltViewModel(assistedFactory = RecordViewModel.Factory::class)
class RecordViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Record,
    clock: Clock,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val envelopeId = route.id

    private val zone = clock.zone
    private val instant = clock.instant()

    private val expenseId = ExpenseId.generate()
    private var submission: Job? = null

    private val _backEvent = Channel<Unit>(Channel.BUFFERED)
    val backEvent = _backEvent.receiveAsFlow()

    private suspend fun sendBack() {
        _backEvent.send(Unit)
    }

    fun submit(amount: Long, note: String) {
        if (submission?.isActive == true || submission?.isCompleted == true) return
        submission = viewModelScope.launch {
            repository.recordExpense(
                ExpenseSubmission(
                    id = expenseId,
                    happenedAt = instant, // TODO: User select
                    happenedZone = zone,
                    allocations = listOf(
                        ExpenseAllocation(
                            expenseId = expenseId,
                            envelopeId = envelopeId,
                            amount = amount,
                            note = note,
                        ),
                    ),
                ),
            )
            sendBack()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Record): RecordViewModel
    }
}