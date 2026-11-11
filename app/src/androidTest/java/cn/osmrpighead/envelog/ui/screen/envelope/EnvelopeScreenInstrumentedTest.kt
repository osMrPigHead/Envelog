package cn.osmrpighead.envelog.ui.screen.envelope

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.time.format
import cn.osmrpighead.envelog.core.time.untilExclusive
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.ExpenditureId
import cn.osmrpighead.envelog.data.model.ExpenditureMetaInfo
import cn.osmrpighead.envelog.data.query.AllocationWithInfo
import cn.osmrpighead.envelog.testing.InstrumentedTestData
import cn.osmrpighead.envelog.testing.hasButtonRole
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EnvelopeScreenInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fieldId = Any()
    private val envelope = Envelope(
        id = InstrumentedTestData.envelopeA,
        name = "餐饮",
        isBudgeted = true,
        archived = false,
    )
    private val state = mutableStateOf<EnvelopeUiState>(successState())

    private var updatedEnvelope: Envelope? = null
    private var openedBudgetId: BudgetId? = null
    private var newBudgetDate: LocalDate? = null
    private var saveBudgetCalls = 0
    private var deleteBudgetCalls = 0
    private var closeBudgetCalls = 0
    private var saveAndBackCalls = 0
    private var archiveAndBackCalls = 0
    private var clearSubmissionErrorCalls = 0

    private val okText: String get() = context.getString(R.string.common_action_ok)
    private val archiveText: String get() = context.getString(R.string.envelope_edit_action_archive)
    private val archiveConfirmText: String get() = context.getString(R.string.common_action_archive)

    @Test
    fun envelope_editingNameReportsUpdate() {
        setScreen()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("外食")

        composeRule.runOnIdle { assertEquals("外食", updatedEnvelope?.name) }
        composeRule.onNode(hasSetTextAction()).assertTextContains("外食")
    }

    @Test
    fun envelope_togglingBudgetHidesBudgetTabAndFallsBackToCalendar() {
        setScreen()
        selectTab(R.string.envelope_edit_tab_budget)

        composeRule.onNode(isToggleable()).performClick()

        composeRule.runOnIdle { assertFalse(updatedEnvelope?.isBudgeted ?: true) }
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_tab_budget)).assertDoesNotExist()
        composeRule.onNodeWithText("15").assertIsDisplayed()

        composeRule.onNode(isToggleable()).performClick()

        composeRule.runOnIdle { assertTrue(updatedEnvelope?.isBudgeted ?: false) }
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_tab_budget)).assertExists()
    }

    @Test
    fun envelope_hidesBudgetTabWhenNotBudgeted() {
        state.value = successState(envelope = envelope.copy(isBudgeted = false))
        setScreen()

        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_tab_budget)).assertDoesNotExist()
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_tab_expenses)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_tab_calendar)).assertIsDisplayed()
    }

    @Test
    fun envelope_defaultsToCalendarTab() {
        setScreen()

        composeRule.onNodeWithText("15").assertIsDisplayed()
    }

    @Test
    fun envelope_clickingDayWithoutBudgetRequestsNewBudget() {
        setScreen()
        selectTab(R.string.envelope_edit_tab_calendar)

        composeRule.onNodeWithText("15").performClick()

        composeRule.runOnIdle { assertEquals(LocalDate.of(2026, 10, 15), newBudgetDate) }
    }

    @Test
    fun envelope_clickingBudgetedDayOpensBudget() {
        val budget = InstrumentedTestData.budget(0xD1)
        state.value = successState(budgets = mapOf(budget.id to budget))
        setScreen()
        selectTab(R.string.envelope_edit_tab_calendar)

        composeRule.onNodeWithText("15").performClick()

        composeRule.runOnIdle { assertEquals(budget.id, openedBudgetId) }
    }

    @Test
    fun envelope_budgetListShowsDateRangeAndAmount() {
        val budget = InstrumentedTestData.budget(0xD6, amount = 12_345).copy(
            dateSpan = LocalDate.of(2026, 10, 5) untilExclusive LocalDate.of(2026, 10, 10),
        )
        state.value = successState(budgets = mapOf(budget.id to budget))
        setScreen()
        selectTab(R.string.envelope_edit_tab_budget)

        val dateRange = context.getString(R.string.time_format_day, 5) +
            context.getString(R.string.symbol_en_dash) +
            context.getString(R.string.time_format_day, 9)
        composeRule.onNodeWithText(dateRange).assertIsDisplayed()
        composeRule.onNodeWithText(budget.amount.formatAsAmount(Formatter.AmountConcise)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_budget_list_date_range)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_budget_list_amount)).assertIsDisplayed()
    }

    @Test
    fun envelope_budgetRowClickOpensBudget() {
        val budget = InstrumentedTestData.budget(0xD7)
        state.value = successState(budgets = mapOf(budget.id to budget))
        setScreen()
        selectTab(R.string.envelope_edit_tab_budget)

        val dateRange = context.getString(R.string.time_format_day, 1) +
            context.getString(R.string.symbol_en_dash) +
            context.getString(R.string.time_format_day, 31)
        composeRule.onNodeWithText(dateRange).performClick()

        composeRule.runOnIdle { assertEquals(budget.id, openedBudgetId) }
    }

    @Test
    fun envelope_expenseListShowsAllocation() {
        state.value = successState(allocationsWithInfo = listOf(allocation(0xC5, 5, 12_345)))
        setScreen()
        selectTab(R.string.envelope_edit_tab_expenses)

        val expenditure = allocation(0xC5, 5, 12_345).expenditure
        val dateTime = expenditure.happenedAt.format(expenditure.happenedZone)
        composeRule.onNodeWithText(dateTime).assertIsDisplayed()
        composeRule.onNodeWithText("123.45").assertIsDisplayed()
    }

    @Test
    fun envelope_budgetSheetSaveReportsCallback() {
        val budget = InstrumentedTestData.budget(0xD2)
        state.value = successState(activeItem = ActiveItem(budget = budget, isNew = false, error = null, complete = false))
        setScreen()
        awaitBudgetSheet()

        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_budget_range_label)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_budget_amount_label)).assertIsDisplayed()

        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).performClick()

        composeRule.runOnIdle { assertEquals(1, saveBudgetCalls) }
    }

    @Test
    fun envelope_budgetSheetDeleteReportsCallback() {
        val budget = InstrumentedTestData.budget(0xD3)
        state.value = successState(activeItem = ActiveItem(budget = budget, isNew = false, error = null, complete = false))
        setScreen()
        awaitBudgetSheet()

        composeRule.onNode(
            hasText(context.getString(R.string.common_action_delete)) and hasClickAction() and hasButtonRole(),
        ).performClick()

        composeRule.runOnIdle { assertEquals(1, deleteBudgetCalls) }
    }

    @Test
    fun envelope_budgetSheetCancelReportsClose() {
        val budget = InstrumentedTestData.budget(0xD4)
        state.value = successState(activeItem = ActiveItem(budget = budget, isNew = false, error = null, complete = false))
        setScreen()
        awaitBudgetSheet()

        composeRule.onNode(
            hasText(context.getString(R.string.common_action_cancel)) and hasClickAction() and hasButtonRole(),
        ).performClick()

        composeRule.runOnIdle { assertEquals(1, closeBudgetCalls) }
    }

    @Test
    fun envelope_budgetSheetShowsAllocationDateTimeAndUnsignedAmount() {
        val budget = InstrumentedTestData.budget(0xD5)
        val allocation = AllocationWithInfo(
            expenditure = ExpenditureMetaInfo(
                id = ExpenditureId(UUID(0L, 0xC1)),
                happenedAt = LocalDate.of(2026, 10, 5).atTime(12, 0)
                    .atZone(InstrumentedTestData.zone).toInstant(),
                happenedDate = LocalDate.of(2026, 10, 5),
                happenedZone = InstrumentedTestData.zone,
            ),
            envelopeId = InstrumentedTestData.envelopeA,
            amount = 12_345,
        )
        state.value = successState(
            activeItem = ActiveItem(
                budget = budget,
                isNew = false,
                error = null,
                complete = false,
                allocations = listOf(allocation),
            ),
        )
        setScreen()
        awaitBudgetSheet()

        val dateTime = allocation.expenditure.happenedAt.format(allocation.expenditure.happenedZone)
        composeRule.onNodeWithText(dateTime).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("123.45").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("+123.45").assertDoesNotExist()
    }

    @Test
    fun envelope_submissionErrorDialogReportsDismiss() {
        state.value = successState(envelopeSubmissionError = EnvelopeSubmissionError.BudgetOverlaps)
        setScreen()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.common_label_error)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_msg_budget_overlaps)).assertIsDisplayed()

        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).performClick()

        composeRule.runOnIdle { assertEquals(1, clearSubmissionErrorCalls) }
        composeRule.onNodeWithText(context.getString(R.string.common_label_error)).assertDoesNotExist()
    }

    @Test
    fun envelope_archiveButtonShowsConfirmationDialog() {
        setScreen()

        clickArchiveButton()

        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_msg_archive_confirm))
            .assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, archiveAndBackCalls) }
    }

    @Test
    fun envelope_archiveConfirmationReportsArchiveAndBack() {
        setScreen()
        clickArchiveButton()

        composeRule.onNode(hasText(archiveConfirmText) and hasClickAction() and hasButtonRole())
            .performClick()

        composeRule.runOnIdle { assertEquals(1, archiveAndBackCalls) }
    }

    @Test
    fun envelope_archiveConfirmationCancelDismissesDialog() {
        setScreen()
        clickArchiveButton()

        composeRule.onNode(hasText(context.getString(R.string.common_action_cancel)) and hasClickAction() and hasButtonRole())
            .performClick()

        composeRule.runOnIdle { assertEquals(0, archiveAndBackCalls) }
        composeRule.onNodeWithText(context.getString(R.string.envelope_edit_msg_archive_confirm))
            .assertDoesNotExist()
    }

    @Test
    fun envelope_topBarBackReportsSaveAndBack() {
        setScreen()

        composeRule.onNodeWithContentDescription(context.getString(R.string.common_action_back)).performClick()

        composeRule.runOnIdle { assertEquals(1, saveAndBackCalls) }
    }

    @Test
    fun envelope_backPressReportsSaveAndBack() {
        setScreen()
        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) { hasEnabledBackCallback() }

        composeRule.runOnUiThread {
            assertEquals(Lifecycle.State.RESUMED, composeRule.activity.lifecycle.currentState)
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { saveAndBackCalls == 1 }
    }

    private fun hasEnabledBackCallback(): Boolean {
        var enabled = false
        composeRule.runOnUiThread {
            enabled = composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks()
        }
        return enabled
    }

    private fun awaitBudgetSheet() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.envelope_edit_budget_range_label))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun clickArchiveButton() {
        composeRule.onNode(hasText(archiveText) and hasClickAction() and hasButtonRole()).performClick()
    }

    private fun selectTab(labelRes: Int) {
        composeRule.onNode(hasText(context.getString(labelRes)) and hasClickAction()).performClick()
        composeRule.waitForIdle()
    }

    private fun allocation(value: Long, day: Int, amount: Long): AllocationWithInfo =
        AllocationWithInfo(
            expenditure = ExpenditureMetaInfo(
                id = ExpenditureId(UUID(0L, value)),
                happenedAt = LocalDate.of(2026, 10, day).atTime(12, 0)
                    .atZone(InstrumentedTestData.zone).toInstant(),
                happenedDate = LocalDate.of(2026, 10, day),
                happenedZone = InstrumentedTestData.zone,
            ),
            envelopeId = InstrumentedTestData.envelopeA,
            amount = amount,
        )

    private fun setScreen() {
        composeRule.setContent {
            EnvelogTheme(dynamicColor = false) {
                EnvelopeScreen(
                    uiState = state.value,
                    initializeForNew = {},
                    updateEnvelope = {
                        updatedEnvelope = it
                        state.value = (state.value as EnvelopeUiState.Success).copy(envelope = it)
                    },
                    openActiveBudget = { openedBudgetId = it },
                    newActiveBudget = { newBudgetDate = it },
                    updateActiveBudget = { budget ->
                        val current = state.value as EnvelopeUiState.Success
                        state.value = current.copy(activeItem = current.activeItem.copy(budget = budget))
                    },
                    closeActiveBudget = { closeBudgetCalls++ },
                    saveActiveBudget = { saveBudgetCalls++ },
                    deleteActiveBudget = { deleteBudgetCalls++ },
                    clearActiveBudget = {},
                    saveAndBack = { saveAndBackCalls++ },
                    archiveAndBack = { archiveAndBackCalls++ },
                    clearSubmissionError = {
                        clearSubmissionErrorCalls++
                        val current = state.value as EnvelopeUiState.Success
                        state.value = current.copy(envelopeSubmissionError = null)
                    },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun successState(
        envelope: Envelope = this.envelope,
        budgets: Map<BudgetId, Budget> = emptyMap(),
        allocationsWithInfo: List<AllocationWithInfo> = emptyList(),
        activeItem: ActiveItem = ActiveItem(budget = null, isNew = false, error = null, complete = true),
        envelopeSubmissionError: EnvelopeSubmissionError? = null,
    ): EnvelopeUiState.Success = EnvelopeUiState.Success(
        envelope = envelope,
        isNew = false,
        yearMonth = InstrumentedTestData.month,
        budgets = budgets,
        amountFieldId = fieldId,
        allocations = emptyList(),
        allocationsWithInfo = allocationsWithInfo,
        activeItem = activeItem,
        envelopeSubmissionError = envelopeSubmissionError,
    )
}
