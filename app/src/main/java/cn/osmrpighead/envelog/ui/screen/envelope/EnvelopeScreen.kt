package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.HugeDangerButton
import cn.osmrpighead.envelog.ui.component.dialog.ArchiveEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.dialog.DeleteEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.form.MonthPickerButton
import cn.osmrpighead.envelog.ui.component.form.MonthPickerSheetBuilder
import cn.osmrpighead.envelog.ui.icon.Icon
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
                    imageVector = Icon.ArrowBack,
                    contentDescription = stringResource(R.string.common_action_back),
                )
            }
        },
        actions = {
            MonthPickerButton(
                yearMonth,
                MonthPickerSheetBuilder.of(selectMonth),
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingExtra)),
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun SuccessBody(
    yearMonth: YearMonth,
    today: LocalDate,
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

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacingBold))) {
            EnvelopeForm(
                form = detail.envelopeForm,
                update = { onEvent(UpdateEnvelope(it)) },
            )

            OutlinedButton(
                onClick = { onEvent(OpenQuickSet) },
                enabled = detail.envelopeForm.envelope.isBudgeted && yearMonth >= YearMonth.from(today),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) { Text(text = stringResource(R.string.envelope_edit_quick_set_title)) }

            SecondaryTabRow(
                selectedTabIndex = visibleTabs.indexOf(activeTab),
                modifier = Modifier.padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) {
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
            modifier = Modifier.weight(1f),
        )

        if (detail.envelopeForm.isNew)
            HugeDangerButton(
                onClick = back,
            ) { Text(text = stringResource(R.string.common_action_cancel)) }
        else if (detail.isDeletable)
            HugeDangerButton(
                onClick = { showDeleteConfirmation = true },
            ) { Text(text = stringResource(R.string.common_action_delete_envelope)) }
        else
            HugeDangerButton(
                onClick = { showArchiveConfirmation = true },
            ) { Text(text = stringResource(R.string.common_action_archive_envelope)) }
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

@Composable
private fun Body(
    yearMonth: YearMonth,
    today: LocalDate,
    detail: EnvelopeDetail,
    onEvent: (EnvelopeEvent) -> Unit,
    back: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { if (detail.saveAndQuit) onEvent(SaveEnvelope) else back() }
    when (detail) {
        is EnvelopeDetail.Success -> SuccessBody(yearMonth, today, detail, onEvent, back, modifier)
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
        close = { onEvent(DismissDetail) },
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
