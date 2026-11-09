package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
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
import cn.osmrpighead.envelog.ui.style.disableVerticalDrag
import cn.osmrpighead.envelog.util.wrapSuspending
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
    saveActiveBudget: (suspend () -> Unit) -> Unit,
    save: () -> Unit,
    cancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler {
        save()
        cancel()
    }

    val uiContext = rememberCoroutineScope().coroutineContext

    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
        ) {

            OutlinedTextField(
                value = uiState.envelope.name,
                onValueChange = { updateEnvelope(uiState.envelope.copy(name = it)) },
                label = { Text(text = stringResource(R.string.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledSwitch(
                checked = uiState.envelope.isBudgeted,
                onCheckedChange = { updateEnvelope(uiState.envelope.copy(isBudgeted = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) { Text(text = stringResource(R.string.envelope_budgeted), style = MaterialTheme.typography.bodyLarge) }

            if (uiState.envelope.isBudgeted) BudgetCalendar(
                model = uiState.calendar,
                onDateClick = newActiveBudget,
                onBudgetClick = openActiveBudget,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            )

            if (uiState.activeBudget != null) {
                val numKeypadHost = rememberNumKeypadHostState()
                val sheetState = rememberModalBottomSheetState()

                LaunchedEffect(sheetState.currentValue) {
                    when (sheetState.currentValue) {
                        PartiallyExpanded -> numKeypadHost.cancel()
                        else -> {}
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = {
                        numKeypadHost.cancel()
                        closeActiveBudget()
                    },
                    sheetState = sheetState,
                ) {
                    LaunchedEffect(sheetState) {
                        sheetState.show()
                    }

                    LaunchedEffect(numKeypadHost.isOpen) {
                        if (numKeypadHost.isOpen) sheetState.expand()
                        else sheetState.partialExpand()
                    }

                    Scaffold(
                        bottomBar = {
                            NumKeypadHost(
                                numKeypadHost,
                                modifier = Modifier.disableVerticalDrag(),
                            )
                        },
                    ) { innerPadding ->
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
                            onSave = { saveActiveBudget(uiContext.wrapSuspending(sheetState::hide)) },
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(dimensionResource(R.dimen.spacingLarge)),
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
    saveActiveBudget: (suspend () -> Unit) -> Unit,
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
