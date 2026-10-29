package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.loadOrNull
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetCalendar
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetItemSheet
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetCalendarModel
import cn.osmrpighead.envelog.util.UUIDZero
import cn.osmrpighead.envelog.util.date.DatePeriod
import cn.osmrpighead.envelog.util.date.toPeriod
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvelopeSheetScreen(
    uiState: EnvelopeSheetUiState,
    onNameChange: (String) -> Unit,
    onBudgetedChange: (Boolean) -> Unit,
    onDateClicked: (LocalDate) -> Unit,
    confirmBudgetItemSheetChange: (SheetValue) -> Boolean,
    onPeriodChange: (DatePeriod) -> Unit,
    onAmountChange: (Long) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
        ) {

            val envelope = uiState.target.loadOrNull { it.envelope } ?: Envelope(
                id = UUIDZero,
                name = stringResource(R.string.envelope_new_name),
                isBudgeted = true,
                archived = false,
            ) // TODO: Loading & NotExisting

            OutlinedTextField(
                value = uiState.fields.name.value ?: envelope.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(text = stringResource(id = R.string.name)) },
            )

            LabeledSwitch(
                checked = uiState.fields.isBudgeted.value ?: envelope.isBudgeted,
                onCheckedChange = onBudgetedChange,
            ) { Text(text = stringResource(R.string.envelope_budgeted), style = MaterialTheme.typography.bodyLarge) }

            if (uiState.fields.isBudgeted.value ?: envelope.isBudgeted) BudgetCalendar(
                period = uiState.yearMonth.toPeriod(),
                model = uiState.target.loadOrNull { it.calendarModel } ?: BudgetCalendarModel(
                    allocations = emptyMap(),
                    budgets = emptyList(),
                ),
                firstDayOfWeek = DayOfWeek.MONDAY,  // TODO: User rule
                onClick = onDateClicked,
            )

            if (uiState.budgetItemSheetModel != null) {
                val numKeypadHost = rememberNumKeypadHostState()
                val sheetState = rememberModalBottomSheetState(confirmValueChange = confirmBudgetItemSheetChange)

                LaunchedEffect(numKeypadHost.isOpen) {
                    if (numKeypadHost.isOpen) sheetState.expand()
                    else if (sheetState.isVisible) sheetState.partialExpand()
                }

                ModalBottomSheet(
                    onDismissRequest = { if (numKeypadHost.isOpen) numKeypadHost.cancel() },
                    sheetState = sheetState,
                ) {
                    Scaffold(bottomBar = { NumKeypadHost(numKeypadHost) }) { innerPadding ->
                        BudgetItemSheet(
                            label = stringResource(R.string.budget_title),
                            numKeypadHost = numKeypadHost,
                            uiState = uiState.budgetItemSheetModel,
                            onPeriodChange = onPeriodChange,
                            onAmountChange = onAmountChange,
                            onSave = onSave,
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
