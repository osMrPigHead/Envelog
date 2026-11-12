package cn.osmrpighead.envelog.ui.screen.overview

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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
import cn.osmrpighead.envelog.testing.hasButtonRole
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
    private val deleted = mutableListOf<EnvelopeId>()
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
        enterManageMode()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_archive))
            .onFirst()
            .assertIsDisplayed()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_edit))
            .onFirst()
            .performClick()
        composeRule.onNodeWithText(context.getString(R.string.overview_action_new_envelope))
            .performScrollTo()
            .performClick()

        assertEquals(listOf(InstrumentedTestData.envelopeA), edited)
        assertEquals(1, newClicks)
        assertEquals(emptyList<EnvelopeId>(), archived)
    }

    @Test
    fun overview_archiveIconShowsConfirmationBeforeArchiving() {
        setScreen(successState())
        enterManageMode()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_archive))
            .onFirst()
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.common_msg_archive_envelope_confirm))
            .assertIsDisplayed()
        assertEquals(emptyList<EnvelopeId>(), archived)

        confirmDialogButton(context.getString(R.string.common_action_archive))

        assertEquals(listOf(InstrumentedTestData.envelopeA), archived)
    }

    @Test
    fun overview_deletableEnvelopeRevealsDeleteIconInsteadOfArchive() {
        setScreen(successState(canDeleteA = true, canDeleteB = false))
        enterManageMode()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_delete))
            .onFirst()
            .assertIsDisplayed()
        // 只有 canDelete 为真的第一个卡片换成删除图标，第二个卡片仍是归档图标
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_delete))
            .assertCountEquals(1)
        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_archive))
            .assertCountEquals(1)
    }

    @Test
    fun overview_deleteConfirmationReportsDelete() {
        setScreen(successState(canDeleteA = true))
        enterManageMode()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_delete))
            .onFirst()
            .performClick()

        composeRule.onNodeWithText(context.getString(R.string.common_msg_delete_envelope_confirm))
            .assertIsDisplayed()
        assertEquals(emptyList<EnvelopeId>(), deleted)

        confirmDialogButton(context.getString(R.string.common_action_delete))

        assertEquals(listOf(InstrumentedTestData.envelopeA), deleted)
    }

    @Test
    fun overview_deleteConfirmationCancelDismissesDialog() {
        setScreen(successState(canDeleteA = true))
        enterManageMode()

        composeRule.onAllNodesWithContentDescription(context.getString(R.string.overview_envelope_action_delete))
            .onFirst()
            .performClick()
        confirmDialogButton(context.getString(R.string.common_action_cancel))

        assertEquals(emptyList<EnvelopeId>(), deleted)
        composeRule.onNodeWithText(context.getString(R.string.common_msg_delete_envelope_confirm))
            .assertDoesNotExist()
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
                    onDeleteClick = { deleted += it },
                    onEditClick = { edited += it },
                    onNewClick = { newClicks++ },
                    onEnvelopeClick = { clicked += it },
                )
            }
        }
    }

    private fun enterManageMode() {
        composeRule.onNodeWithContentDescription(context.getString(R.string.overview_action_manage)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(context.getString(R.string.overview_action_manage_finish))
            .assertIsDisplayed()
    }

    private fun confirmDialogButton(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction() and hasButtonRole()).performClick()
    }

    private fun successState(
        canDeleteA: Boolean = false,
        canDeleteB: Boolean = false,
    ): OverviewUiState.Success = OverviewUiState.Success(
        yearMonth = InstrumentedTestData.month,
        today = InstrumentedTestData.today,
        summaryItems = InstrumentedTestData.summaryItems,
        envelopes = listOf(
            InstrumentedTestData.budgetedEnvelope(canDelete = canDeleteA),
            InstrumentedTestData.nonBudgetedEnvelope(canDelete = canDeleteB),
        ),
    )
}
