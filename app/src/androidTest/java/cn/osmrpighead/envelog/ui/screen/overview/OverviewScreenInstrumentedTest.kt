package cn.osmrpighead.envelog.ui.screen.overview

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.testing.InstrumentedTestData
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverviewScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val archived = mutableListOf<EnvelopeId>()
    private val edited = mutableListOf<EnvelopeId>()
    private val clicked = mutableListOf<EnvelopeId>()
    private var newClicks = 0

    @Test
    fun overview_rendersSummaryAndEnvelopes() {
        setScreen(successState())

        composeRule.onNodeWithText(context.getString(R.string.overview_summary_remaining_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.overview_summary_expense_title)).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.overview_summary_balance_title)).assertIsDisplayed()
        composeRule.onNodeWithText("餐饮", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("购物", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText(150_000L.formatAsAmount(Formatter.AmountStandard)).assertIsDisplayed()
    }

    @Test
    fun overview_manageModeRevealsActionsAndNewCard() {
        setScreen(successState())

        composeRule.onNodeWithContentDescription(context.getString(R.string.overview_action_manage)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(context.getString(R.string.overview_action_manage_finish)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_archive))
            .onFirst()
            .assertIsDisplayed()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_archive))
            .onFirst()
            .performClick()
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_edit))
            .onFirst()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.overview_action_new_envelope))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(InstrumentedTestData.envelopeA), archived)
        assertEquals(listOf(InstrumentedTestData.envelopeA), edited)
        assertEquals(1, newClicks)
    }

    @Test
    fun overview_envelopeClickReportsId() {
        setScreen(successState())

        composeRule.onNodeWithText("餐饮", substring = true).performClick()

        assertEquals(listOf(InstrumentedTestData.envelopeA), clicked)
    }

    @Test
    fun overview_emptyStateShowsNewEnvelopeCard() {
        setScreen(
            OverviewUiState.Success(
                yearMonth = InstrumentedTestData.month,
                today = InstrumentedTestData.today,
                summaryItems = emptyList(),
                envelopes = emptyList(),
            ),
        )

        composeRule.onNodeWithText(context.getString(R.string.overview_action_new_envelope))
            .performScrollTo()
            .performClick()

        assertEquals(1, newClicks)
    }

    private fun setScreen(state: OverviewUiState) {
        composeRule.setContent {
            EnvelogTheme(dynamicColor = false) {
                OverviewScreen(
                    uiState = state,
                    onArchiveClick = { archived += it },
                    onEditClick = { edited += it },
                    onNewClick = { newClicks++ },
                    onEnvelopeClick = { clicked += it },
                )
            }
        }
    }

    private fun successState(): OverviewUiState.Success = OverviewUiState.Success(
        yearMonth = InstrumentedTestData.month,
        today = InstrumentedTestData.today,
        summaryItems = InstrumentedTestData.summaryItems,
        envelopes = listOf(
            InstrumentedTestData.budgetedEnvelope(),
            InstrumentedTestData.nonBudgetedEnvelope(),
        ),
    )
}
