package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    fun request(
        fieldId: Any,
        consumer: NumKeypadConsumer,
        initialValue: String = "0",
    ) {
        session = Session(fieldId, consumer)
        state = NumKeypadState(
            scale = consumer.scale,
            roundingMode = consumer.roundingMode,
            leftOperand = initialValue.toBigDecimalByFormat()?.toPlainString() ?: "0",
        )
    }

    fun updateState(newState: NumKeypadState) {
        if (session != null) state = newState
    }

    fun validate(): Boolean = session?.consumer?.validate(state.evaluate()) ?: false

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

    private fun close() {
        session = null
        state = NumKeypadState()
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
    if (!host.isOpen) return

    BackHandler { host.cancel() }

    Surface(
        modifier = modifier.fillMaxWidth(),
        // TODO: tonalElevation, shadowElevation
    ) {
        NumKeypad(
            state = host.state,
            onStateChange = host::updateState,
            confirmEnabled = host.validate(),
            onConfirm = host::confirm,
            onCancel = host::cancel,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
