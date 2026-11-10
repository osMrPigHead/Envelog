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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.testing.InstrumentedTestData
import cn.osmrpighead.envelog.testing.hasButtonRole
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

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
    private var saveCalls = 0
    private var cancelCalls = 0

    private val okText: String get() = context.getString(R.string.ok)

    @Test
    fun envelope_editingNameReportsUpdate() {
        setScreen()

        composeRule.onNode(hasSetTextAction()).performTextReplacement("外食")

        composeRule.runOnIdle { assertEquals("外食", updatedEnvelope?.name) }
        composeRule.onNode(hasSetTextAction()).assertTextContains("外食")
    }

    @Test
    fun envelope_togglingBudgetReportsUpdateAndHidesCalendar() {
        setScreen()

        composeRule.onNode(isToggleable()).performClick()

        composeRule.runOnIdle { assertFalse(updatedEnvelope?.isBudgeted ?: true) }
        composeRule.onNodeWithText("15").assertDoesNotExist()
    }

    @Test
    fun envelope_clickingDayWithoutBudgetRequestsNewBudget() {
        setScreen()

        composeRule.onNodeWithText("15").performClick()

        composeRule.runOnIdle { assertEquals(LocalDate.of(2026, 10, 15), newBudgetDate) }
    }

    @Test
    fun envelope_clickingBudgetedDayOpensBudget() {
        val budget = InstrumentedTestData.budget(0xD1)
        state.value = successState(budgets = mapOf(budget.id to budget))
        setScreen()

        composeRule.onNodeWithText("15").performClick()

        composeRule.runOnIdle { assertEquals(budget.id, openedBudgetId) }
    }

    @Test
    fun envelope_budgetSheetSaveReportsCallback() {
        val budget = InstrumentedTestData.budget(0xD2)
        state.value = successState(activeBudget = budget)
        setScreen()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(context.getString(R.string.budget_title)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(context.getString(R.string.budget_title)).assertExists()
        composeRule.onNodeWithText(context.getString(R.string.budget_range)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.budget_amount)).assertIsDisplayed()

        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).performClick()

        composeRule.runOnIdle { assertEquals(1, saveBudgetCalls) }
    }

    @Test
    fun envelope_backPressSavesAndCancels() {
        setScreen()
        composeRule.onNode(hasSetTextAction()).assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) { hasEnabledBackCallback() }

        composeRule.runOnUiThread {
            assertEquals(Lifecycle.State.RESUMED, composeRule.activity.lifecycle.currentState)
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) { saveCalls == 1 && cancelCalls == 1 }
    }

    private fun hasEnabledBackCallback(): Boolean {
        var enabled = false
        composeRule.runOnUiThread {
            enabled = composeRule.activity.onBackPressedDispatcher.hasEnabledCallbacks()
        }
        return enabled
    }

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
                    updateActiveBudget = {
                        state.value = (state.value as EnvelopeUiState.Success).copy(activeBudget = it)
                    },
                    closeActiveBudget = { callback ->
                        state.value = (state.value as EnvelopeUiState.Success).copy(activeBudget = null)
                        runBlocking { callback() }
                    },
                    saveActiveBudget = { saveBudgetCalls++ },
                    save = { saveCalls++ },
                    cancel = { cancelCalls++ },
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun successState(
        envelope: Envelope = this.envelope,
        budgets: Map<BudgetId, Budget> = emptyMap(),
        activeBudget: Budget? = null,
    ): EnvelopeUiState.Success = EnvelopeUiState.Success(
        envelope = envelope,
        yearMonth = InstrumentedTestData.month,
        budgets = budgets,
        fieldId = fieldId,
        allocations = emptyList(),
        activeBudget = activeBudget,
        activeIsNew = false,
    )
}
