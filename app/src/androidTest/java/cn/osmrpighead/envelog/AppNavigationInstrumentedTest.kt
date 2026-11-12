package cn.osmrpighead.envelog

import android.content.Context
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.time.HalfOpenDateSpan
import cn.osmrpighead.envelog.core.time.toHalfOpenDateSpan
import cn.osmrpighead.envelog.data.model.Budget
import cn.osmrpighead.envelog.data.model.BudgetId
import cn.osmrpighead.envelog.data.model.Envelope
import cn.osmrpighead.envelog.data.model.EnvelopeId
import cn.osmrpighead.envelog.data.repository.EnvelogRepository
import cn.osmrpighead.envelog.testing.hasButtonRole
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationInstrumentedTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: EnvelogRepository

    @Inject
    lateinit var clock: Clock

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val envelopeName = "测试信封"
    private val newEnvelopeName = "测试新信封"
    private val editedEnvelopeName = "测试外食"

    private lateinit var today: LocalDate
    private lateinit var month: YearMonth
    private lateinit var span: HalfOpenDateSpan
    private var envelopeId: EnvelopeId = EnvelopeId(UUID.randomUUID())

    private val okText: String get() = context.getString(R.string.common_action_ok)
    private val manageText: String get() = context.getString(R.string.overview_action_manage)
    private val editText: String get() = context.getString(R.string.overview_envelope_action_edit)
    private val newText: String get() = context.getString(R.string.overview_action_new_envelope)

    @Before
    fun setUp() {
        hiltRule.inject()
        today = LocalDate.now(clock)
        month = YearMonth.from(today)
        span = month.toHalfOpenDateSpan()

        val initial = runBlocking { repository.observeOverview(today, month, span).first() }
        assertTrue("端到端测试的数据库应为空，实际有 ${initial.size} 个信封", initial.isEmpty())

        envelopeId = EnvelopeId(UUID.randomUUID())
        runBlocking {
            repository.insertEnvelope(
                envelope = Envelope(
                    id = envelopeId,
                    name = envelopeName,
                    isBudgeted = true,
                    archived = false,
                ),
                budgets = listOf(
                    Budget(
                        id = BudgetId(UUID.randomUUID()),
                        envelopeId = envelopeId,
                        dateSpan = span,
                        attributedMonth = month,
                        amount = 100_000,
                    ),
                ),
            )
        }
    }

    @Test
    fun recordFlow_recordsExpenseAndRefreshesOverview() {
        waitForText(envelopeName)
        composeRule.onNodeWithText(envelopeName, substring = true).performClick()
        waitForKeypad()

        clickKey("1")
        clickKey("0")
        clickKey("0")
        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).performClick()

        waitForRepository {
            runBlocking {
                repository.observeOverview(today, month, span).first()
                    .singleOrNull()?.allocated == 10_000L
            }
        }
        waitForText(envelopeName)

        val summaries = runBlocking { repository.observeOverview(today, month, span).first() }
        assertEquals(10_000L, summaries.single().allocated)
    }

    @Test
    fun editFlow_renamesEnvelopeAndRefreshesOverview() {
        waitForText(envelopeName)
        composeRule.onNodeWithContentDescription(manageText).performClick()
        composeRule.onNodeWithContentDescription(editText).performClick()
        waitForEditField()
        composeRule.onNode(hasSetTextAction()).performTextReplacement(editedEnvelopeName)
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        waitForRepository {
            runBlocking { repository.getEnvelope(envelopeId).name == editedEnvelopeName }
        }
        waitForText(editedEnvelopeName)

        assertEquals(editedEnvelopeName, runBlocking { repository.getEnvelope(envelopeId).name })
    }

    @Test
    fun newFlow_createsEnvelopeAndRefreshesOverview() {
        waitForText(envelopeName)
        composeRule.onNodeWithContentDescription(manageText).performClick()
        composeRule.onNodeWithText(newText).performScrollTo().performClick()
        waitForEditField()
        composeRule.onNode(hasSetTextAction()).performTextReplacement(newEnvelopeName)
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        waitForRepository {
            runBlocking {
                repository.observeOverview(today, month, span).first()
                    .any { it.name == newEnvelopeName }
            }
        }
        waitForText(newEnvelopeName)

        assertEquals(
            newEnvelopeName,
            runBlocking {
                repository.observeOverview(today, month, span).first()
                    .single { it.name == newEnvelopeName }
                    .name
            },
        )
    }

    private fun clickKey(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction() and hasButtonRole()).performClick()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForEditField() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForKeypad() {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForRepository(timeoutMillis: Long = 5_000, condition: suspend () -> Boolean) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) { runBlocking { condition() } }
    }
}
