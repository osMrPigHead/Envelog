package cn.osmrpighead.envelog.ui.screen.record

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.data.model.ExpenditureSubmission
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import com.fasterxml.uuid.NoArgGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.time.Clock

@HiltViewModel(assistedFactory = RecordViewModel.Factory::class)
class RecordViewModel @AssistedInject constructor(
    @Assisted route: AppRoute.Record,
    @Assisted private val onCancel: () -> Unit,
    appState: AppStateHolder,
    clock: Clock,
    uuidGenerator: NoArgGenerator,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val envelopeId = route.id

    private val today = appState.today.value
    private val zone = clock.zone
    private val instant = clock.instant()

    private val expenditureId = ExpenditureId(uuidGenerator.generate())
    private var submission: Job? = null

    private val host = NumKeypadHostState()
    private val fieldId = Any()
    private val consumer = AmountConsumer(2, RoundingMode.HALF_UP, ::submit, onCancel)

    private val _uiState = mutableStateOf(
        RecordUiState(
            numKeypadHost = host,
            amount = 0,
            amountConsumer = consumer,
            fieldId = fieldId,
        ),
    )
    val uiState: RecordUiState by _uiState

    fun submit(amount: Long) {
        if (submission?.isActive == true || submission?.isCompleted == true) return
        submission = viewModelScope.launch {
            repository.recordExpenditure(
                ExpenditureSubmission(
                    id = expenditureId,
                    happenedAt = instant,
                    happenedDate = today,
                    happenedZone = zone,
                    allocations = listOf(
                        ExpenditureAllocation(
                            expenditureId = expenditureId,
                            envelopeId = envelopeId,
                            amount = amount,
                        ),
                    ),
                ),
            )
            _uiState.value = _uiState.value.copy(amount = amount)
            onCancel()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: AppRoute.Record, cancel: () -> Unit): RecordViewModel
    }
}