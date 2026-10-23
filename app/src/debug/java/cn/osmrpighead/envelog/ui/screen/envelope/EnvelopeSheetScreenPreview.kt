package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureEnvelopeAllocation
import cn.osmrpighead.envelog.data.model.Transaction
import cn.osmrpighead.envelog.data.model.TransactionType
import cn.osmrpighead.envelog.data.query.AllocationResult
import cn.osmrpighead.envelog.data.query.EnvelopeResult
import cn.osmrpighead.envelog.ui.model.Field
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.date.untilExclusive
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@LangPreview
@Composable
private fun EnvelopeSheetScreenPreview() {
    EnvelogTheme(dynamicColor = false) {
        var uiState by remember { mutableStateOf(previewUiState) }
        EnvelopeScreen(
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
            transaction = Transaction(
                id = transactionId,
                type = TransactionType.Expenditure,
                dateTime = date.atTime(12, 0).atZone(ZoneOffset.UTC),
                amount = amount,
            ),
            allocation = ExpenditureEnvelopeAllocation(
                transactionId = transactionId,
                envelopeId = previewEnvelopeId,
                amount = amount,
            ),
        )
    }

private val previewUiState: EnvelopeSheetUiState.Success = EnvelopeSheetUiState.Success(
    value = EnvelopeResult(
        envelope = Envelope(
            id = previewEnvelopeId,
            name = "Food",
            isBudgeted = true,
            archived = false,
        ),
        budgets = EnvelopePreviewData.mixedBudgets,
        allocations = previewAllocations,
    ),
    yearMonth = YearMonth.of(2026, 9),
    calendarModel = EnvelopePreviewData.mixedCalendar,
    fields = EnvelopeSheetFields(),
    budgetItem = null,
)
