package cn.osmrpighead.envelog.ui.screen.overview.component

import android.content.Context
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.data.model.ExpenseSubmission
import cn.osmrpighead.envelog.testing.hasButtonRole
import cn.osmrpighead.envelog.testing.InstrumentedTestData
import cn.osmrpighead.envelog.ui.model.FormState
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecordModalBottomSheetInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var submitted: ExpenseSubmission? = null
    private var closeCalls = 0

    private val finishText: String get() = context.getString(R.string.common_action_finish)
    private val cancelText: String get() = context.getString(R.string.common_action_cancel)

    @Before
    fun setUp() {
        submitted = null
        closeCalls = 0
        composeRule.setContent {
            EnvelogTheme(dynamicColor = false) {
                RecordModalBottomSheet(
                    formState = FormState.Active(Unit),
                    selectedEnvelope = InstrumentedTestData.envelopeA,
                    envelopes = listOf(
                        InstrumentedTestData.budgetedEnvelope(),
                        InstrumentedTestData.nonBudgetedEnvelope(),
                    ),
                    selectEnvelope = {},
                    submit = { submitted = it },
                    close = { closeCalls++ },
                )
            }
        }
        // 弹层挂载后立即向键盘宿主发起会话，无需点击输入框
        composeRule.waitForIdle()
    }

    @Test
    fun recordSheet_roundsHalfUpAndSubmitsCents() {
        clickKey("1")
        clickKey("2")
        clickKey(".")
        clickKey("3")
        clickKey("4")
        clickKey("5")
        composeRule.onNode(hasSetTextAction()).performTextReplacement("聚餐")

        composeRule.onNode(hasText(finishText) and hasClickAction() and hasButtonRole())
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            val allocation = checkNotNull(submitted).allocations.single()
            assertEquals(1235L, allocation.amount)
            assertEquals(InstrumentedTestData.envelopeA, allocation.envelopeId)
            assertEquals("聚餐", allocation.note)
        }
        assertEquals(0, closeCalls)
    }

    @Test
    fun recordSheet_zeroAmountCannotSubmit() {
        composeRule.onNode(hasText(finishText) and hasClickAction() and hasButtonRole()).assertIsNotEnabled()

        clickKey("0")

        composeRule.onNode(hasText(finishText) and hasClickAction() and hasButtonRole()).assertIsNotEnabled()
        composeRule.runOnIdle { assertNull(submitted) }
    }

    @Test
    fun recordSheet_cancelClosesSheetWithoutSubmit() {
        clickKey("5")
        composeRule.onNodeWithContentDescription(cancelText).performClick()

        composeRule.runOnIdle { assertNull(submitted) }
        composeRule.waitUntil(timeoutMillis = 5_000) { closeCalls == 1 }
    }

    private fun clickKey(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction() and hasButtonRole()).performClick()
    }
}
