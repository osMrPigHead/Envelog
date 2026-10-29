package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetCalendar
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetItemSheet
import cn.osmrpighead.envelog.core.time.DatePeriod
import cn.osmrpighead.envelog.core.time.toPeriod
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Success(
    uiState: EnvelopeSheetUiState.Success,
    onNameChange: (String) -> Unit,
    onBudgetedChange: (Boolean) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    onBudgetItemSheetDismissed: () -> Unit,
    onPeriodChange: (DatePeriod) -> Unit,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
        ) {

            OutlinedTextField(
                value = uiState.fields.name.value ?: uiState.value.envelope.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.name)) },
            )

            LabeledSwitch(
                checked = uiState.fields.isBudgeted.value ?: uiState.value.envelope.isBudgeted,
                onCheckedChange = onBudgetedChange,
            ) { Text(text = stringResource(R.string.envelope_budgeted), style = MaterialTheme.typography.bodyLarge) }

            if (uiState.fields.isBudgeted.value ?: uiState.value.envelope.isBudgeted) BudgetCalendar(
                period = uiState.yearMonth.toPeriod(),
                model = uiState.calendarModel,
                firstDayOfWeek = DayOfWeek.MONDAY,  // TODO: User rule
                onClick = onDateClicked,
            )

            if (uiState.budgetItem != null) {
                val numKeypadHost = rememberNumKeypadHostState()
                val sheetState = rememberModalBottomSheetState()

                LaunchedEffect(numKeypadHost.isOpen) {
                    if (numKeypadHost.isOpen) sheetState.expand()
                    else if (sheetState.isVisible) sheetState.partialExpand()
                }

                ModalBottomSheet(
                    onDismissRequest = {
                        if (numKeypadHost.isOpen) numKeypadHost.cancel()
                        else onBudgetItemSheetDismissed()
                    },
                    sheetState = sheetState,
                ) {
                    Scaffold(bottomBar = { NumKeypadHost(numKeypadHost) }) { innerPadding ->
                        BudgetItemSheet(
                            label = stringResource(R.string.budget_title),
                            numKeypadHost = numKeypadHost,
                            uiState = uiState.budgetItem,
                            onPeriodChange = onPeriodChange,
                            onAmountChange = onAmountChange,
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(horizontal = dimensionResource(R.dimen.spacingBold))
                                .fillMaxWidth(),
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun EnvelopeSheetScreen(
    uiState: EnvelopeSheetUiState,
    onNameChange: (String) -> Unit,
    onBudgetedChange: (Boolean) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    onBudgetItemSheetDismissed: () -> Unit,
    onPeriodChange: (DatePeriod) -> Unit,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EnvelopeSheetUiState.Loading -> {}  // TODO Loading & NoSuch
        EnvelopeSheetUiState.NoSuchEnvelope -> {}
        is EnvelopeSheetUiState.Success -> Success(
            uiState = uiState,
            onNameChange = onNameChange,
            onBudgetedChange = onBudgetedChange,
            onDateClicked = onDateClicked,
            onBudgetItemSheetDismissed = onBudgetItemSheetDismissed,
            onPeriodChange = onPeriodChange,
            onAmountChange = onAmountChange,
            modifier = modifier,
        )
    }
}
