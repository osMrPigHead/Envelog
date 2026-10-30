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
import cn.osmrpighead.envelog.data.db.entity.BudgetEntity
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetFields
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import java.time.LocalDate
import java.util.UUID

@LangPreview
@Composable
private fun BudgetItemSheetPreview() {
    EnvelogTheme(dynamicColor = false) {
        var budget by remember { mutableStateOf(previewBudget) }
        val host = rememberNumKeypadHostState()
        val uiState = BudgetItemSheetModel(
            value = budget,
            fields = BudgetItemSheetFields(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            BudgetItemSheet(
                label = "Budget",
                numKeypadHost = host,
                uiState = uiState,
                onPeriodChange = { budget = budget.copy(startDate = it.start, endDate = it.endExclusive) },
                onAmountChange = { budget = budget.copy(amount = it) },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
            NumKeypadHost(host)
        }
    }
}

private val previewBudget: BudgetEntity = BudgetEntity(
    id = UUID(0L, 3L),
    envelopeId = UUID.fromString("00000000-0000-0000-0000-000000000101"),
    startDate = LocalDate.of(2026, 9, 1),
    endDate = LocalDate.of(2026, 9, 8),
    amount = 30_000,
)
