package cn.osmrpighead.envelog.ui.model

data class Field<T>(
    val value: T? = null,
    val submitState: SubmitState = SubmitState.Idle,
) {
    fun toMutable(): MutableField<T> = MutableField(value, submitState)
}
