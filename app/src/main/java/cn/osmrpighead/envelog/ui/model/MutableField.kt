package cn.osmrpighead.envelog.ui.model

class MutableField<T>(
    var value: T? = null,
    var submitState: SubmitState = SubmitState.Idle
) {
    fun toField(): Field<T> = Field(value, submitState)
}