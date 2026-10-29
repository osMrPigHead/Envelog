package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.FieldWithState
import cn.osmrpighead.envelog.ui.model.ModelTarget
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.EnvelopePreviewData
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.time.LocalDate
import java.util.UUID

@LangPreview
@Composable
private fun BudgetItemSheetPreview() = BudgetItemSheetSample(
    target = ModelTarget.Existing(previewBudget),
    today = EnvelopePreviewData.today,
)

@LangPreview
@Composable
private fun BudgetItemSheetNewPreview() = BudgetItemSheetSample(
    target = ModelTarget.New(),
    today = EnvelopePreviewData.today,
)

@Composable
private fun BudgetItemSheetSample(target: ModelTarget<Budget>, today: LocalDate) {
    EnvelogTheme(dynamicColor = false) {
        var fields by remember { mutableStateOf(BudgetItemSheetFields()) }
        val host = rememberNumKeypadHostState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            BudgetItemSheet(
                label = "Budget",
                numKeypadHost = host,
                uiState = BudgetItemSheetModel(
                    target = target,
                    fields = fields,
                    today = today,
                ),
                onPeriodChange = { period ->
                    fields = fields.copy(period = FieldWithState(period))
                },
                onAmountChange = { amount ->
                    fields = fields.copy(amount = FieldWithState(amount))
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
            NumKeypadHost(host)
        }
    }
}

private val previewBudget: Budget = Budget(
    id = UUID(0L, 3L),
    envelopeId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
    startDate = LocalDate.of(2026, 9, 1),
    endDate = LocalDate.of(2026, 9, 8),
    amount = 30_000,
)
