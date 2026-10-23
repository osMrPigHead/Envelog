package cn.osmrpighead.envelog.ui.model

sealed interface SubmitState {
    data object Idle : SubmitState
    data object Submitting : SubmitState
    data class Failed(val exception: RuntimeException): SubmitState
}