package cn.osmrpighead.envelog.ui.component.form

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.numkeypad.EvaluatedResult
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHost
import cn.osmrpighead.envelog.ui.component.numkeypad.rememberNumKeypadHostState
import cn.osmrpighead.envelog.ui.preview.LangPreview
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.theme.EnvelogTheme
import cn.osmrpighead.envelog.util.asAmount
import java.math.BigDecimal
import java.math.RoundingMode

@LangPreview
@Composable
private fun NumKeypadFieldPreview() {
    EnvelogTheme(dynamicColor = false) {
        var amount by remember { mutableLongStateOf(30_000L) }
        val fieldId = remember { Any() }
        val consumer = remember { PreviewAmountConsumer { amount = it } }

        val host = rememberNumKeypadHostState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacingBold)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacingLarge)),
        ) {
            NumKeypadField(
                value = amount.formatAsAmount(Formatter.AmountInteger),
                host = host,
                consumer = consumer,
                fieldId = fieldId,
                label = { Text("Amount") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.weight(1f))
            NumKeypadHost(host)
        }
    }
}

private class PreviewAmountConsumer(
    private val onAmountChange: (Long) -> Unit,
) : NumKeypadConsumer {
    override val scale: Int = 0
    override val roundingMode: RoundingMode = RoundingMode.HALF_UP

    override fun validate(result: EvaluatedResult): Boolean =
        result is EvaluatedResult.Success && result.value > BigDecimal.ZERO

    override fun onConfirm(result: EvaluatedResult) {
        onAmountChange((result as EvaluatedResult.Success).value.asAmount(scale, roundingMode))
    }

    override fun onCancel() {}
}
