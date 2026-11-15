package cn.osmrpighead.envelog.ui.screen.envelope.feature

import cn.osmrpighead.envelog.data.model.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 新建信封尚未落库时的内存预算集合；DetailModalDelegate/QuickSetDelegate 写，EnvelopeFormDelegate 经 budgets 读。 */
class BudgetDraftStore {
    private val budgets = MutableStateFlow(emptyList<Budget>())

    val state: StateFlow<List<Budget>> = budgets.asStateFlow()

    fun current(): List<Budget> = budgets.value

    fun update(transform: (List<Budget>) -> List<Budget>) = budgets.update(transform)
}
