package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.osmrpighead.envelog.data.query.DateRange
import cn.osmrpighead.envelog.data.query.DateRangeRule
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.data.repository.EnvelogRepositoryException
import cn.osmrpighead.envelog.ui.model.EnvelogViewModel
import cn.osmrpighead.envelog.ui.navigation.Screen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

class EnvelopeViewModel(
    private val route: Screen.Envelope,
    shared: EnvelogViewModel,
    private val repository: EnvelogRepository,
) : ViewModel() {
    private val yearMonth = shared.yearMonth

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<EnvelopeUiState> = yearMonth.flatMapLatest { yearMonth ->
        val range = DateRange(
            start = yearMonth.atDay(1),
            end = yearMonth.plusMonths(1).atDay(1),
            rule = DateRangeRule.StartInRange,
        )
        try {
            repository.observeEnvelopeWithExtra(
                envelopeId = route.id,
                range = range,
            ).map { result -> EnvelopeUiState.Success(result, yearMonth) }
        } catch (_: EnvelogRepositoryException.ArgumentException.NoSuch.Envelope) {
            flowOf(EnvelopeUiState.NoSuchEnvelope)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        EnvelopeUiState.Loading,
    )
}