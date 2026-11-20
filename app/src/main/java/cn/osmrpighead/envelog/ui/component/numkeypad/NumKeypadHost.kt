package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cn.osmrpighead.envelog.core.locale.AppLocale
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

    fun setEnabled(enabled: Boolean) {
        updateState(state.copy(enabled = enabled))
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
    val visibility = remember { MutableTransitionState(false) }
    visibility.targetState = host.isOpen

    BackHandler(visibility.targetState) { if (host.isOpen) host.cancel() }

    BottomAnimatedNumKeypad(
        visibleState = visibility,
        state = host.state,
        onStateChange = host::updateState,
        confirmEnabled = host.confirmEnabled,
        onConfirm = host::confirm,
        onCancel = host::cancel,
        modifier = modifier,
    )
}
