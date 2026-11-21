package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.DangerButton
import cn.osmrpighead.envelog.ui.component.dialog.ArchiveEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.dialog.DeleteEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.OutlinedYearMonthButton
import cn.osmrpighead.envelog.ui.icon.Icons
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.screen.envelope.component.*
import cn.osmrpighead.envelog.ui.screen.envelope.component.quickset.QuickSetBudgetModalBottomSheet
import java.time.LocalDate
import java.time.YearMonth

private val EnvelopeDetail.saveAndQuit
    get() = when (this) {
        is EnvelopeDetail.Success -> this.envelopeForm.saveBeforeQuit
        else -> false
    }

val EnvelopeTab.labelRes: Int
    get() = when (this) {
        EnvelopeTab.Calendar -> R.string.envelope_edit_tab_calendar
        EnvelopeTab.Budget -> R.string.envelope_edit_tab_budget
        EnvelopeTab.Allocation -> R.string.envelope_edit_tab_expenses
    }

@Composable
fun EnvelopeScreen(
    uiState: EnvelopeUiState,
    onEvent: (EnvelopeEvent) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                yearMonth = uiState.yearMonth,
                saveBeforeQuit = uiState.detail.saveAndQuit,
                selectMonth = { onEvent(SelectMonth(it)) },
                saveEnvelope = { onEvent(SaveEnvelope) },
                back = back,
            )
        },
    ) { innerPadding ->
        Body(
            yearMonth = uiState.yearMonth,
            today = uiState.today,
            detail = uiState.detail,
            onEvent = onEvent,
            back = back,
            modifier = Modifier.padding(innerPadding),
        )
        Overlays(
            yearMonth = uiState.yearMonth,
            detail = uiState.detail,
            onEvent = onEvent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    yearMonth: YearMonth,
    saveBeforeQuit: Boolean,
    selectMonth: (YearMonth) -> Unit,
    saveEnvelope: () -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = stringResource(R.string.envelope_edit_title)) },
        navigationIcon = {
            IconButton(onClick = if (saveBeforeQuit) saveEnvelope else back) {
                Icon(
                    imageVector = Icons.ArrowBack,
                    contentDescription = stringResource(R.string.common_action_back),
                )
            }
        },
        actions = {
            OutlinedYearMonthButton(
                yearMonth = yearMonth,
                onYearMonthChange = selectMonth,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.space20)),
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Body(
    yearMonth: YearMonth,
    today: LocalDate,
    detail: EnvelopeDetail,
    onEvent: (EnvelopeEvent) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissed by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.isImeVisible
    BackHandler {
        if (imeVisible) return@BackHandler
        if (detail.saveAndQuit) onEvent(SaveEnvelope) else {
            dismissed = true
            back()
        }
    }
    when (detail) {
        is EnvelopeDetail.Success -> SuccessBody(yearMonth, today, dismissed, detail, onEvent, back, modifier)
        EnvelopeDetail.Loading -> {}
        EnvelopeDetail.IsNew -> {
            val envelopeName = stringResource(R.string.envelope_edit_default_name)
            LaunchedEffect(Unit) { onEvent(InitializeNewEnvelope(envelopeName)) }
        }
    }
}

@Composable
private fun Overlays(
    yearMonth: YearMonth,
    detail: EnvelopeDetail,
    onEvent: (EnvelopeEvent) -> Unit,
) {
    if (detail !is EnvelopeDetail.Success) return

    if (detail.detailModalState != null) DetailModalBottomSheet(
        detailModalState = detail.detailModalState,
        update = { onEvent(UpdateBudget(it)) },
        dismiss = { onEvent(DismissDetail) },
        save = { onEvent(SaveBudget) },
        delete = { onEvent(DeleteBudget) },
        clear = { onEvent(ClearDetail) },
    )

    if (detail.quickSetFormModalState != null) QuickSetBudgetModalBottomSheet(
        yearMonth = yearMonth,
        modalState = detail.quickSetFormModalState,
        update = { onEvent(UpdateQuickSet(it)) },
        apply = { onEvent(ApplyQuickSet(it)) },
        dismiss = { onEvent(DismissQuickSet) },
        clear = { onEvent(ClearQuickSet) },
        cancelOverwrite = { onEvent(CancelOverwrite) },
    )

    if (detail.envelopeForm.state is FormState.Active) when (detail.envelopeForm.state.error) {
        EnvelopeSubmissionError.BudgetOverlaps -> BudgetOverlapsErrorDialog { onEvent(DismissEnvelopeFormError) }
        else -> {}
    }
}

@Composable
private fun SuccessBody(
    yearMonth: YearMonth,
    today: LocalDate,
    dismissed: Boolean,
    detail: EnvelopeDetail.Success,
    onEvent: (EnvelopeEvent) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(detail.envelopeForm.state) {
        if (detail.envelopeForm.state == FormState.Completed) back()
    }

    var selectedTab by rememberSaveable { mutableStateOf(EnvelopeTab.Calendar) }
    val visibleTabs =
        if (detail.envelopeForm.envelope.isBudgeted) EnvelopeTab.entries
        else EnvelopeTab.entries - EnvelopeTab.Budget
    val activeTab = selectedTab.takeIf { it in visibleTabs } ?: visibleTabs.first()

    var showArchiveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.padding(top = dimensionResource(R.dimen.space4))) {
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space16))) {
            EnvelopeForm(
                yearMonth = yearMonth,
                today = today,
                form = detail.envelopeForm,
                dismissed = dismissed,
                update = { onEvent(UpdateEnvelope(it)) },
                quickset = { onEvent(OpenQuickSet) },
                modifier = Modifier,
            )

            SecondaryTabRow(selectedTabIndex = visibleTabs.indexOf(activeTab)) {
                for (tab in visibleTabs) Tab(
                    selected = activeTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(text = stringResource(tab.labelRes)) },
                )
            }
        }

        Ledger(
            yearMonth = yearMonth,
            ledgerState = detail.ledgerState,
            activeTab = activeTab,
            onBudgetClick = { id, date -> onEvent(OpenDetailWithExistingBudget(id, date)) },
            onDateClick = { onEvent(OpenDetailCreatingBudget(it)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.space16)),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space8)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.space20),
                    vertical = dimensionResource(R.dimen.space4),
                ),
        ) {
            if (detail.envelopeForm.isNew)
                DangerButton(
                    onClick = back,
                    modifier = Modifier.weight(1f),
                ) { Text(text = stringResource(R.string.common_action_cancel)) }
            else if (detail.isDeletable)
                DangerButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                ) { Text(text = stringResource(R.string.common_action_delete_envelope)) }
            else
                DangerButton(
                    onClick = { showArchiveConfirmation = true },
                    modifier = Modifier.weight(1f),
                ) { Text(text = stringResource(R.string.common_action_archive_envelope)) }
            FilledTonalButton(
                onClick = { onEvent(SaveEnvelope) },
                modifier = Modifier.weight(1f),
            ) { Text(text = stringResource(R.string.common_action_save_and_back)) }
        }
    }

    if (showArchiveConfirmation) ArchiveEnvelopeConfirmationDialog(
        onConfirm = {
            showArchiveConfirmation = false
            onEvent(ArchiveEnvelope)
        },
        onDismiss = { showArchiveConfirmation = false },
    )

    if (showDeleteConfirmation) DeleteEnvelopeConfirmationDialog(
        onConfirm = {
            showDeleteConfirmation = false
            onEvent(DeleteEnvelope)
        },
        onDismiss = { showDeleteConfirmation = false },
    )
}
