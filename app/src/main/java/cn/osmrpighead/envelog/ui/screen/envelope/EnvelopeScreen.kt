package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetCalendar
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetItemModalSheet
import cn.osmrpighead.envelog.ui.screen.envelope.component.EnvelopeSubmissionErrorDialog
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
    deleteActiveBudget: () -> Unit,
    clearActiveBudget: () -> Unit,
    saveAndBack: () -> Unit,
    clearSubmissionError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { saveAndBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(R.string.envelope_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = saveAndBack) {
                        Icon(
                            imageVector = Icon.ArrowBack,
                            contentDescription = stringResource(R.string.common_action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
        ) {

            OutlinedTextField(
                value = uiState.envelope.name,
                onValueChange = { updateEnvelope(uiState.envelope.copy(name = it)) },
                label = { Text(text = stringResource(R.string.common_label_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledSwitch(
                checked = uiState.envelope.isBudgeted,
                onCheckedChange = { updateEnvelope(uiState.envelope.copy(isBudgeted = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(
                    text = stringResource(R.string.envelope_edit_label_budgeted),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (uiState.envelope.isBudgeted) BudgetCalendar(
                model = uiState.calendar,
                onDateClick = newActiveBudget,
                onBudgetClick = openActiveBudget,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            )

            BudgetItemModalSheet(
                activeBudget = uiState.activeBudget,
                amountFieldId = uiState.amountFieldId,
                update = updateActiveBudget,
                close = closeActiveBudget,
                save = saveActiveBudget,
                delete = deleteActiveBudget,
                clear = clearActiveBudget,
            )

            if (uiState.envelopeSubmissionError != null)
                EnvelopeSubmissionErrorDialog(
                    error = uiState.envelopeSubmissionError,
                    onDismiss = clearSubmissionError,
                )
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
    deleteActiveBudget: () -> Unit,
    clearActiveBudget: () -> Unit,
    saveAndBack: () -> Unit,
    clearSubmissionError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EnvelopeUiState.Loading -> {}
        EnvelopeUiState.IsNew -> {
            initializeForNew(
                Envelope(
                    id = EnvelopeId.generate(),
                    name = stringResource(R.string.envelope_edit_default_name),
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
            deleteActiveBudget,
            clearActiveBudget,
            saveAndBack,
            clearSubmissionError,
            modifier,
        )
    }
}
