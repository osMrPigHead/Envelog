package cn.osmrpighead.envelog.data.model

data class ExpenseAllocation(
    val expenseId: ExpenseId,
    val envelopeId: EnvelopeId,
    val amount: Long,
)
