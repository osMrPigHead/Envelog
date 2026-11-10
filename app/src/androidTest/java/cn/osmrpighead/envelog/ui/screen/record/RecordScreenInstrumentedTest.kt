package cn.osmrpighead.envelog.ui.screen.record

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.testing.hasButtonRole
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.RoundingMode

@RunWith(AndroidJUnit4::class)
class RecordScreenInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val host = NumKeypadHostState()
    private val fieldId = Any()
    private val amountState = mutableStateOf(0L)
    private var submitted: Long? = null
    private var cancelled = 0

    private val okText: String get() = context.getString(R.string.common_action_ok)
    private val cancelText: String get() = context.getString(R.string.common_action_cancel)

    @Before
    fun setUp() {
        submitted = null
        cancelled = 0
        val consumer = AmountConsumer(
            scale = 2,
            roundingMode = RoundingMode.HALF_UP,
            submit = {
                submitted = it
                amountState.value = it
            },
            cancel = { cancelled++ },
        )
        composeRule.setContent {
            EnvelogTheme(dynamicColor = false) {
                RecordScreen(
                    uiState = RecordUiState(
                        numKeypadHost = host,
                        amount = amountState.value,
                        amountConsumer = consumer,
                        fieldId = fieldId,
                    ),
                    back = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.waitForIdle()
        openKeypad()
    }

    /**
     * 通过点击输入框打开键盘，而不是提前调用 [NumKeypadHostState.request]。
     *
     * `NumKeypadField` 在“未获得焦点”时会关闭会话，而输入框第一次挂载时会收到一次 `Inactive`
     * 焦点事件；若在组合前就打开会话，这次事件会把会话关掉，键盘随即播放退出动画消失。
     */
    private fun openKeypad() {
        // 金额为零时输入框显示的就是 Formatter.AmountStandard 的结果（定点两位小数）
        composeRule.onNode(hasText("0.00") and hasClickAction()).performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun recordScreen_roundsHalfUpAndSubmitsCents() {
        clickKey("1")
        clickKey("2")
        clickKey(".")
        clickKey("3")
        clickKey("4")
        clickKey("5")

        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole())
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1235L, submitted)
            assertEquals(0, cancelled)
        }
    }

    @Test
    fun recordScreen_zeroAmountCannotSubmit() {
        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).assertIsNotEnabled()

        clickKey("0")

        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).assertIsNotEnabled()
        composeRule.runOnIdle { assertNull(submitted) }
    }

    @Test
    fun recordScreen_cancelDiscardsInput() {
        clickKey("5")
        composeRule.onNodeWithContentDescription(cancelText).performClick()

        composeRule.runOnIdle {
            assertEquals(1, cancelled)
            assertNull(submitted)
        }
    }

    private fun clickKey(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction() and hasButtonRole()).performClick()
    }
}
