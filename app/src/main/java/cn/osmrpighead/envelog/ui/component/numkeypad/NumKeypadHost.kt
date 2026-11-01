package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.core.locale.AppLocale
import cn.osmrpighead.envelog.ui.theme.MotionTokens
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.ParsePosition

fun String?.toBigDecimalByFormat(): BigDecimal? {
    val s = this?.trim() ?: return null
    if (s.isEmpty()) return null
    val pos = ParsePosition(0)
    val format = DecimalFormat("###,##0.##########", DecimalFormatSymbols(AppLocale.current()))
        .apply { isParseBigDecimal = true }
    val result = format.parse(s, pos) as? BigDecimal ?: return null
    return if (pos.index == s.length) result else null
}

class NumKeypadHostState {
    private var session: Session? by mutableStateOf(null)

    val isOpen: Boolean get() = session != null
    val activeFieldId: Any? get() = session?.fieldId

    var state: NumKeypadState by mutableStateOf(NumKeypadState())
        private set

    /** 确认按钮是否可用。会话结束后保留最后一次的值，使键盘播放退出动画时按钮不会变灰 */
    var confirmEnabled: Boolean by mutableStateOf(false)
        private set

    fun request(
        fieldId: Any,
        consumer: NumKeypadConsumer,
        initialValue: String = "0",
    ) {
        session = Session(fieldId, consumer)
        publish(
            NumKeypadState(
                scale = consumer.scale,
                roundingMode = consumer.roundingMode,
                leftOperand = initialValue.toBigDecimalByFormat()?.toPlainString() ?: "0",
            ),
            consumer,
        )
    }

    fun updateState(newState: NumKeypadState) {
        val session = session ?: return
        publish(newState, session.consumer)
    }

    fun safeClose() {
        confirm()
        cancel()
    }

    fun confirm() {
        val session = session ?: return
        val result = state.evaluate()
        if (!session.consumer.validate(result)) return

        close()
        session.consumer.onConfirm(result)
    }

    fun cancel() {
        val session = session ?: return
        close()
        session.consumer.onCancel()
    }

    private fun publish(newState: NumKeypadState, consumer: NumKeypadConsumer) {
        state = newState
        confirmEnabled = consumer.validate(newState.evaluate())
    }

    /** 结束会话，但保留 [state] 与 [confirmEnabled]，供键盘播放退出动画 */
    private fun close() {
        session = null
    }
}

private data class Session(
    val fieldId: Any,
    val consumer: NumKeypadConsumer,
)

@Composable
fun rememberNumKeypadHostState(): NumKeypadHostState = remember { NumKeypadHostState() }

@Composable
fun NumKeypadHost(
    host: NumKeypadHostState,
    modifier: Modifier = Modifier,
) {
    // 初次组合时键盘可能已经打开，因此从隐藏开始播放上滑动画
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = host.isOpen

    BackHandler(enabled = host.isOpen) { host.cancel() }

    AnimatedVisibility(
        visibleState = visibility,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = MotionTokens.DurationKeypadSlide),
            initialOffsetY = { it },
        ) + expandVertically(
            animationSpec = tween(durationMillis = MotionTokens.DurationKeypadSlide),
            expandFrom = Alignment.Bottom,
        ),
        exit = slideOutVertically(
            animationSpec = tween(durationMillis = MotionTokens.DurationKeypadSlide),
            targetOffsetY = { it },
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = MotionTokens.DurationKeypadSlide),
            shrinkTowards = Alignment.Bottom,
        ),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            // TODO: tonalElevation, shadowElevation
        ) {
            NumKeypad(
                state = host.state,
                onStateChange = host::updateState,
                confirmEnabled = host.confirmEnabled,
                onConfirm = host::confirm,
                onCancel = host::cancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
