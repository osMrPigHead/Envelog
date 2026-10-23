package cn.osmrpighead.envelog.ui.screen.envelope

import cn.osmrpighead.envelog.data.query.EnvelopeResult
import java.time.YearMonth

sealed interface EnvelopeUiState {
    data object Loading : EnvelopeUiState
    data object NoSuchEnvelope: EnvelopeUiState
    data class Success(
        val value: EnvelopeResult,
        val yearMonth: YearMonth,
    ) : EnvelopeUiState
}