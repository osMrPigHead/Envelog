package cn.osmrpighead.envelog.ui.screen.overview

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.ui.component.dialog.ArchiveEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.dialog.DeleteEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.form.MonthPickerButton
import cn.osmrpighead.envelog.ui.component.form.MonthPickerSheetBuilder
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.overview.component.EnvelopeCard
import cn.osmrpighead.envelog.ui.screen.overview.component.PromptBar
import cn.osmrpighead.envelog.ui.screen.overview.component.RecordModalBottomSheet
import cn.osmrpighead.envelog.ui.screen.overview.component.Summary
import cn.osmrpighead.envelog.ui.style.animateItemWithToken
import cn.osmrpighead.envelog.ui.theme.AppTheme
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewScreen(
    uiState: OverviewUiState,
    selectMonth: (YearMonth) -> Unit,
    archiveEnvelope: (EnvelopeId) -> Unit,
    deleteEnvelope: (EnvelopeId) -> Unit,
    requireExpenseFormState: () -> Unit,
    submitExpense: (ExpenseSubmission) -> Unit,
    editEnvelope: (EnvelopeId) -> Unit,
    newEnvelope: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        modifier = Modifier.padding(start = dimensionResource(R.dimen.space8)),
                    )
                },
                actions = {
                    MonthPickerButton(
                        yearMonth = uiState.yearMonth,
                        sheetBuilder = MonthPickerSheetBuilder.of(selectMonth),
                        modifier = Modifier.padding(end = dimensionResource(R.dimen.space20)),
                    )
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        when (uiState.loadState) {
            LoadState.Loading -> {}
            is LoadState.Success -> SuccessBody(
                uiState.today,
                uiState.expenseFormState,
                uiState.loadState,
                archiveEnvelope,
                deleteEnvelope,
                requireExpenseFormState,
                submitExpense,
                editEnvelope,
                newEnvelope,
                Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun SuccessBody(
    today: LocalDate,
    expenseFormState: FormState<Unit>,
    state: LoadState.Success,
    archiveEnvelope: (EnvelopeId) -> Unit,
    deleteEnvelope: (EnvelopeId) -> Unit,
    requireExpenseFormState: () -> Unit,
    submitExpense: (ExpenseSubmission) -> Unit,
    editEnvelope: (EnvelopeId) -> Unit,
    newEnvelope: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var managing by rememberSaveable { mutableStateOf(false) }
    var archiveTarget by remember { mutableStateOf<EnvelopeId?>(null) }
    var deleteTarget by remember { mutableStateOf<EnvelopeId?>(null) }
    var selectedEnvelope by remember { mutableStateOf<EnvelopeId?>(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space8)),
    ) {
        Summary(
            headline = state.headlineSummary,
            vice = state.viceSummary,
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space24)),
        )
        PromptBar(
            managing = managing,
            onManageButtonClick = { managing = !managing },
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space12)),
        )
        LazyColumn(
            modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space12)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space16)),
            contentPadding = PaddingValues(bottom = dimensionResource(R.dimen.space16)),
        ) {
            items(state.envelopes, key = { it.id.toString() }) {
                EnvelopeCard(
                    model = it,
                    today = today,
                    managing = managing,
                    diffColor = { diffAmount ->
                        if (diffAmount < 0) MaterialTheme.colorScheme.error
                        else AppTheme.colorScheme.positive
                    },  // TODO: User rule
                    onClick = {
                        requireExpenseFormState()
                        selectedEnvelope = it.id
                    },
                    onArchiveClick = { archiveTarget = it.id },
                    onDeleteClick = { deleteTarget = it.id },
                    onEditClick = { editEnvelope(it.id) },
                    modifier = Modifier.animateItemWithToken(),
                )
            }
            if (state.envelopes.isEmpty() || managing) item(key = 0) {
                NewEnvelope(onClick = newEnvelope, modifier = Modifier.animateItemWithToken())
            }
        }
    }

    archiveTarget?.let { id ->
        ArchiveEnvelopeConfirmationDialog(
            onConfirm = {
                archiveTarget = null
                archiveEnvelope(id)
            },
            onDismiss = { archiveTarget = null },
        )
    }

    deleteTarget?.let { id ->
        DeleteEnvelopeConfirmationDialog(
            onConfirm = {
                deleteTarget = null
                deleteEnvelope(id)
            },
            onDismiss = { deleteTarget = null },
        )
    }

    selectedEnvelope?.let { id ->
        RecordModalBottomSheet(
            formState = expenseFormState,
            selectedEnvelope = id,
            envelopes = state.envelopes,
            selectEnvelope = { selectedEnvelope = it },
            submit = submitExpense,
            close = { selectedEnvelope = null },
        )
    }
}

@Composable
fun NewEnvelope(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .padding(dimensionResource(R.dimen.space16))
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space4)),
        ) {
            Icon(
                imageVector = Icon.AddCircle,
                contentDescription = null,
                modifier = Modifier.size(
                    with(LocalDensity.current) {
                        MaterialTheme.typography.titleLarge.lineHeight.toDp()
                    },
                ),
            )
            Text(
                text = stringResource(R.string.overview_action_new_envelope),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal),
            )
        }
    }
}
