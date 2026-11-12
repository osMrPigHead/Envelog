package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.ClosedDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.ui.component.HugeDangerButton
import cn.osmrpighead.envelog.ui.component.dialog.ArchiveEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.dialog.DeleteEnvelopeConfirmationDialog
import cn.osmrpighead.envelog.ui.component.form.LabeledSwitch
import cn.osmrpighead.envelog.ui.component.form.MonthPickerButton
import cn.osmrpighead.envelog.ui.component.form.MonthPickerConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.MonthPickerSheetBuilder
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.screen.envelope.component.*
import cn.osmrpighead.envelog.ui.screen.envelope.component.quickset.QuickSetBudgetModalBottomSheet
import cn.osmrpighead.envelog.ui.screen.envelope.model.EnvelopeCalendarModel
import cn.osmrpighead.envelog.ui.screen.envelope.model.QuickSetConfig
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

private enum class EnvelopeTab { Calendar, Budget, Allocation }

private val EnvelopeTab.labelRes: Int
    get() = when (this) {
        EnvelopeTab.Calendar -> R.string.envelope_edit_tab_calendar
        EnvelopeTab.Budget -> R.string.envelope_edit_tab_budget
        EnvelopeTab.Allocation -> R.string.envelope_edit_tab_expenses
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SuccessTopBar(
    yearMonth: YearMonth,
    selectMonth: (YearMonth) -> Unit,
    saveAndBack: () -> Unit,
    modifier: Modifier = Modifier,

) {
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
        actions = {
            MonthPickerButton(
                yearMonth,
                MonthPickerSheetBuilder.Default(MonthPickerConfirmEventHandler.Default(selectMonth)),
                modifier = Modifier.padding(end = dimensionResource(R.dimen.spacingExtra)),
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun EnvelopeTopBar(
    uiState: EnvelopeUiState,
    selectMonth: (YearMonth) -> Unit,
    saveAndBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EnvelopeUiState.IsNew -> {}
        EnvelopeUiState.Loading -> {}
        is EnvelopeUiState.Success -> SuccessTopBar(uiState.yearMonth, selectMonth, saveAndBack, modifier)
    }
}

@Composable
private fun SuccessBody(
    envelope: Envelope,
    isNew: Boolean,
    canDelete: Boolean,
    yearMonth: YearMonth,
    today: LocalDate,
    budgets: Map<BudgetId, Budget>,
    allocationsWithInfo: List<AllocationWithInfo>,
    calendar: EnvelopeCalendarModel,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    updateEnvelope: (Envelope) -> Unit,
    openActiveBudget: (BudgetId) -> Unit,
    newActiveBudget: (LocalDate) -> Unit,
    saveAndBack: () -> Unit,
    archiveAndBack: () -> Unit,
    deleteAndBack: () -> Unit,
    openQuickSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { backEvent.collect { back() } }
    BackHandler { saveAndBack() }

    var selectedTab by rememberSaveable { mutableStateOf(EnvelopeTab.Calendar) }
    var showArchiveConfirmation by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val visibleTabs =
        if (envelope.isBudgeted) EnvelopeTab.entries
        else EnvelopeTab.entries - EnvelopeTab.Budget
    val activeTab = selectedTab.takeIf { it in visibleTabs } ?: visibleTabs.first()

    Column(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacingBold))) {
            OutlinedTextField(
                value = envelope.name,
                onValueChange = { updateEnvelope(envelope.copy(name = it)) },
                label = { Text(text = stringResource(R.string.common_label_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LabeledSwitch(
                checked = envelope.isBudgeted,
                onCheckedChange = { updateEnvelope(envelope.copy(isBudgeted = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(
                    text = stringResource(R.string.envelope_edit_label_budgeted),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            if (envelope.isBudgeted) OutlinedButton(
                onClick = openQuickSet,
                enabled = yearMonth >= YearMonth.from(today),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimensionResource(R.dimen.spacingMedium)),
            ) {
                Text(text = stringResource(R.string.envelope_edit_quick_set_title))
            }

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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.spacingMedium))
                .verticalScroll(rememberScrollState()),
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    val enter = slideInHorizontally(
                        animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                        initialOffsetX = { width -> direction * width / 4 },
                    ) + fadeIn(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                    val exit = slideOutHorizontally(
                        animationSpec = tween(durationMillis = MotionTokens.DurationSwitchSlide),
                        targetOffsetX = { width -> -direction * width / 4 },
                    ) + fadeOut(animationSpec = tween(durationMillis = MotionTokens.DurationSwitchFade))
                    (enter togetherWith exit).using(null)
                },
                label = "EnvelopeTabContent",
            ) { tab ->
                when (tab) {
                    EnvelopeTab.Budget -> BudgetList(
                        budgets = budgets.values,
                        yearMonth = yearMonth,
                        onBudgetClick = openActiveBudget,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.spacingMedium),
                            bottom = dimensionResource(R.dimen.spacingBold),
                        ),
                    )

                    EnvelopeTab.Allocation -> AllocationList(
                        allocations = allocationsWithInfo,
                        modifier = Modifier.padding(
                            top = dimensionResource(R.dimen.spacingMedium),
                            bottom = dimensionResource(R.dimen.spacingBold),
                        ),
                    )

                    EnvelopeTab.Calendar -> BudgetCalendar(
                        model = calendar,
                        onDateClick = newActiveBudget,
                        onBudgetClick = openActiveBudget,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
                    )
                }
            }
        }

            if (isNew)
                HugeDangerButton(
                    onClick = back,
                ) { Text(text = stringResource(R.string.common_action_cancel)) }
            else if (canDelete)
                HugeDangerButton(
                    onClick = { showDeleteConfirmation = true },
                ) { Text(text = stringResource(R.string.envelope_edit_action_delete)) }
            else
                HugeDangerButton(
                    onClick = { showArchiveConfirmation = true },
                ) { Text(text = stringResource(R.string.envelope_edit_action_archive)) }
        }

    if (showArchiveConfirmation) ArchiveEnvelopeConfirmationDialog(
        onConfirm = {
            showArchiveConfirmation = false
            archiveAndBack()
        },
        onDismiss = { showArchiveConfirmation = false },
    )

    if (showDeleteConfirmation) DeleteEnvelopeConfirmationDialog(
        onConfirm = {
            showDeleteConfirmation = false
            deleteAndBack()
        },
        onDismiss = { showDeleteConfirmation = false },
    )
}

@Composable
private fun EnvelopeBody(
    uiState: EnvelopeUiState,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    initializeForNew: (Envelope) -> Unit,
    updateEnvelope: (Envelope) -> Unit,
    openActiveBudget: (BudgetId) -> Unit,
    newActiveBudget: (LocalDate) -> Unit,
    saveAndBack: () -> Unit,
    archiveAndBack: () -> Unit,
    deleteAndBack: () -> Unit,
    openQuickSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        EnvelopeUiState.Loading -> {}
        EnvelopeUiState.IsNew -> {
            val newEnvelope = Envelope(
                id = EnvelopeId.generate(),
                name = stringResource(R.string.envelope_edit_default_name),
                isBudgeted = true,
                archived = false,
            )
            LaunchedEffect(Unit) { initializeForNew(newEnvelope) }
        }

        is EnvelopeUiState.Success -> SuccessBody(
            uiState.envelope,
            uiState.isNew,
            uiState.canDelete,
            uiState.yearMonth,
            uiState.today,
            uiState.budgets,
            uiState.allocationsWithInfo,
            uiState.calendar,
            back,
            backEvent,
            updateEnvelope,
            openActiveBudget,
            newActiveBudget,
            saveAndBack,
            archiveAndBack,
            deleteAndBack,
            openQuickSet,
            modifier,
        )
    }
}

@Composable
private fun EnvelopeOverlays(
    uiState: EnvelopeUiState,
    updateActiveBudget: (Budget) -> Unit,
    closeActiveBudget: () -> Unit,
    saveActiveBudget: () -> Unit,
    deleteActiveBudget: () -> Unit,
    clearActiveBudget: () -> Unit,
    clearSubmissionError: () -> Unit,
    dismissQuickSet: () -> Unit,
    applyQuickSet: (QuickSetConfig, ClosedDateSpan) -> Unit,
    confirmQuickSetOverwrite: () -> Unit,
    cancelQuickSetOverwrite: () -> Unit,
    clearQuickSet: () -> Unit,
) {
    if (uiState !is EnvelopeUiState.Success) return

    DatesModalBottomSheet(
        activeItem = uiState.activeItem,
        amountFieldId = uiState.amountFieldId,
        update = updateActiveBudget,
        close = closeActiveBudget,
        save = saveActiveBudget,
        delete = deleteActiveBudget,
        clear = clearActiveBudget,
    )

    if (uiState.quickSet.open) QuickSetBudgetModalBottomSheet(
        yearMonth = uiState.yearMonth,
        complete = uiState.quickSet.complete,
        conflict = uiState.quickSet.conflict,
        error = uiState.quickSet.error,
        onDismissRequest = dismissQuickSet,
        onDismissed = clearQuickSet,
        onApply = applyQuickSet,
        onConfirmOverwrite = confirmQuickSetOverwrite,
        onCancelOverwrite = cancelQuickSetOverwrite,
    )

    if (uiState.envelopeSubmissionError != null) EnvelopeSubmissionErrorDialog(
        error = uiState.envelopeSubmissionError,
        onDismiss = clearSubmissionError,
    )
}

@Composable
fun EnvelopeScreen(
    uiState: EnvelopeUiState,
    back: () -> Unit,
    backEvent: Flow<Unit>,
    selectMonth: (YearMonth) -> Unit,
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
    archiveAndBack: () -> Unit,
    deleteAndBack: () -> Unit,
    clearSubmissionError: () -> Unit,
    openQuickSet: () -> Unit,
    dismissQuickSet: () -> Unit,
    applyQuickSet: (QuickSetConfig, ClosedDateSpan) -> Unit,
    confirmQuickSetOverwrite: () -> Unit,
    cancelQuickSetOverwrite: () -> Unit,
    clearQuickSet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier,
            topBar = {
                EnvelopeTopBar(uiState, selectMonth, saveAndBack)
            },
        ) { innerPadding ->
            EnvelopeBody(
                uiState,
                back,
                backEvent,
                initializeForNew,
                updateEnvelope,
                openActiveBudget,
                newActiveBudget,
                saveAndBack,
                archiveAndBack,
                deleteAndBack,
                openQuickSet,
                Modifier.padding(innerPadding),
            )
        }
        EnvelopeOverlays(
            uiState,
            updateActiveBudget,
            closeActiveBudget,
            saveActiveBudget,
            deleteActiveBudget,
            clearActiveBudget,
            clearSubmissionError,
            dismissQuickSet,
            applyQuickSet,
            confirmQuickSetOverwrite,
            cancelQuickSetOverwrite,
            clearQuickSet,
        )
    }
}
