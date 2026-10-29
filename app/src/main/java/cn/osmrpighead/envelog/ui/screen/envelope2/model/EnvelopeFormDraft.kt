package cn.osmrpighead.envelog.ui.screen.envelope2.model

data class EnvelopeFormDraft(
    val name: String,
    val budgeted: Boolean,
    val budgets: List<BudgetFormDraft>,
)