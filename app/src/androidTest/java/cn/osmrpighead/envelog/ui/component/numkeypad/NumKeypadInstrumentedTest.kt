package cn.osmrpighead.envelog.ui.component.numkeypad

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.testing.hasButtonRole
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class NumKeypadInstrumentedTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val state = mutableStateOf(NumKeypadState())
    private var confirmed = 0

    private val okText: String get() = context.getString(R.string.ok)

    @Test
    fun keypad_evaluatesChainedArithmetic() {
        setKeypad()
        clickKey("1")
        clickKey(context.getString(R.string.numkeypad_plus))
        clickKey("2")
        clickKey(context.getString(R.string.numkeypad_times))
        clickKey("4")

        composeRule.onNodeWithText("= 12").assertIsDisplayed()
        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).performClick()
        composeRule.runOnIdle { assertEquals(1, confirmed) }
    }

    @Test
    fun keypad_divisionByZeroShowsErrorAndBlocksConfirm() {
        setKeypad()
        clickKey("1")
        clickKey(context.getString(R.string.numkeypad_divides))
        clickKey("0")

        composeRule.onNodeWithText(context.getString(R.string.numkeypad_divided_by_zero)).assertIsDisplayed()
        composeRule.onNode(hasText(okText) and hasClickAction() and hasButtonRole()).assertIsNotEnabled()
        composeRule.runOnIdle { assertEquals(0, confirmed) }
    }

    private fun setKeypad() {
        confirmed = 0
        state.value = NumKeypadState()
        composeRule.setContent {
            EnvelogTheme(dynamicColor = false) {
                val current = state.value
                NumKeypad(
                    state = current,
                    onStateChange = { state.value = it },
                    confirmEnabled = current.evaluate().let { result ->
                        result is EvaluatedResult.Success && result.value > BigDecimal.ZERO
                    },
                    onConfirm = { confirmed++ },
                    onCancel = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    private fun clickKey(text: String) {
        composeRule.onNode(hasText(text) and hasClickAction() and hasButtonRole()).performClick()
    }
}
