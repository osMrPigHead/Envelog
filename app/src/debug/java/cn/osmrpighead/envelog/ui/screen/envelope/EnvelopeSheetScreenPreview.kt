package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.data.db.entity.EnvelopeEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureAllocationEntity
import cn.osmrpighead.envelog.data.db.entity.ExpenditureEntity
import cn.osmrpighead.envelog.data.db.entity.EntryType
import cn.osmrpighead.envelog.data.query_old.AllocationResult
import cn.osmrpighead.envelog.data.query_old.EnvelopeResult
import cn.osmrpighead.envelog.ui.model.Field
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.core.time.untilExclusive
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@LangPreview
@Composable
private fun EnvelopeSheetSheetScreenPreview() {
    EnvelogTheme(dynamicColor = false) {
        var uiState by remember { mutableStateOf(previewUiState) }
        EnvelopeSheetScreen(
            uiState = uiState,
            onNameChange = { name ->
                uiState = uiState.copy(fields = uiState.fields.copy(name = Field(name)))
            },
            onBudgetedChange = { budgeted ->
                uiState = uiState.copy(fields = uiState.fields.copy(isBudgeted = Field(budgeted)))
            },
            onDateClicked = { date ->
                uiState.value.budgets
                    .find { date in (it.startDate untilExclusive it.endDate) }
                    ?.let { budget ->
                        uiState = uiState.copy(
                            budgetItem = BudgetItemSheetModel(value = budget, fields = BudgetItemSheetFields()),
                        )
                    }
            },
            onBudgetItemSheetDismissed = {
                uiState = uiState.copy(budgetItem = null)
            },
            onPeriodChange = { period ->
                val item = uiState.budgetItem
                if (item != null) uiState = uiState.copy(
                    budgetItem = item.copy(
                        value = item.value.copy(startDate = period.start, endDate = period.endExclusive),
                    ),
                )
            },
            onAmountChange = { amount ->
                val item = uiState.budgetItem
                if (item != null) uiState = uiState.copy(
                    budgetItem = item.copy(
                        value = item.value.copy(amount = amount),
                    ),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val previewEnvelopeId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000101")

private val previewAllocations: List<AllocationResult> =
    EnvelopePreviewData.allocations.entries.mapIndexed { index, (date, amount) ->
        val transactionId = UUID(2L, index.toLong())
        AllocationResult(
            transaction = ExpenditureEntity(
                id = transactionId,
                type = EntryType.Expenditure,
                happenedAt = date.atTime(12, 0).atZone(ZoneOffset.UTC),
                amount = amount,
            ),
            allocation = ExpenditureAllocationEntity(
                expenditureId = transactionId,
                envelopeId = previewEnvelopeId,
                amount = amount,
            ),
        )
    }

private val previewUiState: EnvelopeSheetUiState.Success = EnvelopeSheetUiState.Success(
    value = EnvelopeResult(
        envelope = EnvelopeEntity(
            id = previewEnvelopeId,
            name = "Food",
            isBudgeted = true,
            archivedAt = false,
        ),
        budgets = EnvelopePreviewData.mixedBudgets,
        allocations = previewAllocations,
    ),
    yearMonth = YearMonth.of(2026, 9),
    calendarModel = EnvelopePreviewData.mixedCalendar,
    fields = EnvelopeSheetFields(),
    budgetItem = null,
)
