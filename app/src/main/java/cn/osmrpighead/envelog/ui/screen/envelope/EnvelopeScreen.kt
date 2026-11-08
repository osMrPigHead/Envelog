package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetCalendar
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetItemSheet
import java.math.RoundingMode
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Success(
    uiState: EnvelopeUiState.Success,
    updateEnvelope: (Envelope) -> Unit,
    openActiveBudget: (BudgetId) -> Unit,
    newActiveBudget: (LocalDate) -> Unit,
    updateActiveBudget: (Budget) -> Unit,
    closeActiveBudget: () -> Unit,
    saveActiveBudget: () -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        save()
        cancel()
    }

    Scaffold(modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {

            OutlinedTextField(
                value = uiState.envelope.name,
                onValueChange = { updateEnvelope(uiState.envelope.copy(name = it)) },
            )

            LabeledSwitch(
                checked = uiState.envelope.isBudgeted,
                onCheckedChange = { updateEnvelope(uiState.envelope.copy(isBudgeted = it)) },
            ) { Text(text = stringResource(R.string.envelope_budgeted), style = MaterialTheme.typography.bodyLarge) }

            if (uiState.envelope.isBudgeted) BudgetCalendar(
                model = uiState.calendar,
                onDateClick = newActiveBudget,
                onBudgetClick = openActiveBudget,
            )

            if (uiState.activeBudget != null) {
                val numKeypadHost = rememberNumKeypadHostState()
                val sheetState = rememberModalBottomSheetState()

                LaunchedEffect(numKeypadHost.isOpen) {
                    if (!sheetState.isVisible) return@LaunchedEffect
                    if (numKeypadHost.isOpen) sheetState.expand()
                    else sheetState.partialExpand()
                }

                ModalBottomSheet(
                    onDismissRequest = {
                        numKeypadHost.cancel()
                        closeActiveBudget();
                    },
                    sheetState = sheetState,
                ) {
                    Scaffold(bottomBar = { NumKeypadHost(numKeypadHost) }) { innerPadding ->
                        BudgetItemSheet(
                            budget = uiState.activeBudget,
                            onSpanChange = { updateActiveBudget(uiState.activeBudget.copy(dateSpan = it)) },
                            numKeypadHost = numKeypadHost,
                            amountConsumer = AmountConsumer(
                                scale = 0,
                                roundingMode = RoundingMode.HALF_UP,
                                submit = { updateActiveBudget(uiState.activeBudget.copy(amount = it)) },
                                cancel = {},
                            ),
                            fieldId = uiState.fieldId,
                            onSave = saveActiveBudget,
                            modifier = Modifier.padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EnvelopeScreen(
    uiState: EnvelopeUiState,
    initializeForNew: (Envelope) -> Unit,
    updateEnvelope: (Envelope) -> Unit,
    openActiveBudget: (BudgetId) -> Unit,
    newActiveBudget: (LocalDate) -> Unit,
    updateActiveBudget: (Budget) -> Unit,
    closeActiveBudget: () -> Unit,
    saveActiveBudget: () -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EnvelopeUiState.Loading -> {}
        EnvelopeUiState.IsNew -> {
            initializeForNew(
                Envelope(
                    id = EnvelopeId.generate(),
                    name = stringResource(R.string.envelope_manage_new),
                    isBudgeted = true,
                    archived = false,
                ),
            )
        }

        is EnvelopeUiState.Success -> Success(
            uiState,
            updateEnvelope,
            openActiveBudget,
            newActiveBudget,
            updateActiveBudget,
            closeActiveBudget,
            saveActiveBudget,
            save,
            cancel,
            modifier,
        )
    }
}
