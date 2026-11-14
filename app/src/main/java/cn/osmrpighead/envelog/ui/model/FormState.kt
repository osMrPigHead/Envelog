package cn.osmrpighead.envelog.ui.model

sealed interface FormState<out E> {
    data class Active<E>(val error: E?) : FormState<E>
    data object Busy : FormState<Nothing>
    data object Completed : FormState<Nothing>
}