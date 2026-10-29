package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.ui.model.FieldWithState
import cn.osmrpighead.envelog.ui.model.ModelTarget
import cn.osmrpighead.envelog.ui.model.loadOrNull
import cn.osmrpighead.envelog.ui.model.modelTargetExistingOrNew
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeSheetModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.date.untilExclusive

@LangPreview
@Composable
private fun EnvelopeSheetScreenPreview() = EnvelopeSheetScreenSample(previewUiState)

@LangPreview
@Composable
private fun EnvelopeSheetScreenNewPreview() = EnvelopeSheetScreenSample(previewNewUiState)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvelopeSheetScreenSample(initial: EnvelopeSheetUiState) {
    EnvelogTheme(dynamicColor = false) {
        var uiState by remember { mutableStateOf(initial) }
        val budgets = uiState.target.loadOrNull { it.calendarModel.budgets }.orEmpty()

        EnvelopeSheetScreen(
            uiState = uiState,
            onNameChange = { name ->
                uiState = uiState.copy(fields = uiState.fields.copy(name = FieldWithState(name)))
            },
            onBudgetedChange = { budgeted ->
                uiState = uiState.copy(fields = uiState.fields.copy(isBudgeted = FieldWithState(budgeted)))
            },
            onDateClicked = { date ->
                uiState = uiState.copy(
                    budgetItemSheetModel = BudgetItemSheetModel(
                        target = modelTargetExistingOrNew(
                            budgets.find { date in it.startDate untilExclusive it.endDate },
                        ),
                        fields = BudgetItemSheetFields(),
                        today = EnvelopePreviewData.today,
                    ),
                )
            },
            confirmBudgetItemSheetChange = {
                uiState = uiState.copy(budgetItemSheetModel = null)
            },
            onPeriodChange = { period ->
                val item = uiState.budgetItemSheetModel
                if (item != null) uiState = uiState.copy(
                    budgetItemSheetModel = item.copy(
                        fields = item.fields.copy(period = FieldWithState(period)),
                    ),
                )
            },
            onAmountChange = { amount ->
                val item = uiState.budgetItemSheetModel
                if (item != null) uiState = uiState.copy(
                    budgetItemSheetModel = item.copy(
                        fields = item.fields.copy(amount = FieldWithState(amount)),
                    ),
                )
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private val previewUiState: EnvelopeSheetUiState = EnvelopeSheetUiState(
    target = ModelTarget.Existing(
        EnvelopeSheetModel(
            envelope = EnvelopePreviewData.envelope,
            calendarModel = EnvelopePreviewData.mixedCalendar,
        ),
    ),
    yearMonth = EnvelopePreviewData.yearMonth,
    fields = EnvelopeSheetFields(),
    budgetItemSheetModel = null,
)

private val previewNewUiState: EnvelopeSheetUiState = EnvelopeSheetUiState(
    target = ModelTarget.New(),
    yearMonth = EnvelopePreviewData.yearMonth,
    fields = EnvelopeSheetFields(),
    budgetItemSheetModel = null,
)
