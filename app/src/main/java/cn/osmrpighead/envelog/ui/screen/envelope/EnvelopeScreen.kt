package cn.osmrpighead.envelog.ui.screen.envelope

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import cn.osmrpighead.envelog.ui.screen.envelope.component.AllocationList
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetCalendar
import cn.osmrpighead.envelog.ui.screen.envelope.component.BudgetList
import cn.osmrpighead.envelog.ui.screen.envelope.component.DatesModalBottomSheet
import cn.osmrpighead.envelog.ui.screen.envelope.component.EnvelopeSubmissionErrorDialog
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.time.LocalDate

private enum class EnvelopeTab { Calendar, Budget, Allocation }

private val EnvelopeTab.labelRes: Int
    get() = when (this) {
        EnvelopeTab.Calendar -> R.string.envelope_edit_tab_calendar
        EnvelopeTab.Budget -> R.string.envelope_edit_tab_budget
        EnvelopeTab.Allocation -> R.string.envelope_edit_tab_expenses
    }

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

    var selectedTab by rememberSaveable { mutableStateOf(EnvelopeTab.Calendar) }
    val visibleTabs =
        if (uiState.envelope.isBudgeted) EnvelopeTab.entries
        else EnvelopeTab.entries - EnvelopeTab.Budget
    val activeTab = selectedTab.takeIf { it in visibleTabs } ?: visibleTabs.first()

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
                .fillMaxSize(),
        ) {
            Column(modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.spacingBold))) {
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
                            budgets = uiState.budgets.values,
                            yearMonth = uiState.yearMonth,
                            onBudgetClick = openActiveBudget,
                            modifier = Modifier.padding(
                                top = dimensionResource(R.dimen.spacingMedium),
                                bottom = dimensionResource(R.dimen.spacingBold),
                            ),
                        )

                        EnvelopeTab.Allocation -> AllocationList(
                            allocations = uiState.allocationsWithInfo,
                            modifier = Modifier.padding(
                                top = dimensionResource(R.dimen.spacingMedium),
                                bottom = dimensionResource(R.dimen.spacingBold),
                            ),
                        )

                        EnvelopeTab.Calendar -> BudgetCalendar(
                            model = uiState.calendar,
                            onDateClick = newActiveBudget,
                            onBudgetClick = openActiveBudget,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = dimensionResource(R.dimen.spacingBold)),
                        )
                    }
                }
            }
        }

        DatesModalBottomSheet(
            activeItem = uiState.activeItem,
            amountFieldId = uiState.amountFieldId,
            update = updateActiveBudget,
            close = closeActiveBudget,
            save = saveActiveBudget,
            delete = deleteActiveBudget,
            clear = clearActiveBudget,
        )

        if (uiState.envelopeSubmissionError != null) EnvelopeSubmissionErrorDialog(
            error = uiState.envelopeSubmissionError,
            onDismiss = clearSubmissionError,
        )
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
