package cn.osmrpighead.envelog.ui.screen.record

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.core.navigation.AppRoute
import cn.osmrpighead.envelog.core.number.asAmount
import cn.osmrpighead.envelog.data.model.Expenditure
import cn.osmrpighead.envelog.data.model.ExpenditureAllocation
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.ui.component.numkeypad.EvaluatedResult
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AppStateHolder
import com.fasterxml.uuid.NoArgGenerator
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock

private class Consumer(
    private val submit: (Long) -> Unit,
    private val cancel: () -> Unit,
) : NumKeypadConsumer {
    override val scale: Int = 2
    override val roundingMode: RoundingMode = RoundingMode.HALF_UP

    override fun validate(result: EvaluatedResult): Boolean =
        result is EvaluatedResult.Success && result.value > BigDecimal.ZERO

    override fun onConfirm(result: EvaluatedResult) {
        require(result is EvaluatedResult.Success)
        submit(result.value.asAmount(scale, roundingMode))
    }

    override fun onCancel() {
        cancel()
    }
}

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
    private val consumer = Consumer(::submit, onCancel)

    private val _uiState = mutableStateOf(
        RecordUiState(
            numKeypadHost = host,
            amount = 0,
            numKeypadConsumer = consumer,
            fieldId = fieldId,
        ),
    )
    val uiState: RecordUiState by _uiState

    init {
        host.request(fieldId, consumer)
    }

    fun submit(amount: Long) {
        if (submission?.isActive == true || submission?.isCompleted == true) return
        submission = viewModelScope.launch {
            repository.recordExpenditure(
                Expenditure(
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