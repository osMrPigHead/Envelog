package cn.osmrpighead.envelog.ui.model

data class FormState<D, S>(
    val draft: D?,
    val states: S,
    val isLoading: Boolean,
    val isSaving: Boolean,
    val isInitialized: Boolean,
    val canSave: Boolean,
)
