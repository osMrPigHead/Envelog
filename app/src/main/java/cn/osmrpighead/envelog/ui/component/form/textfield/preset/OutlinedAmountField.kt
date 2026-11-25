package cn.osmrpighead.envelog.ui.component.form.textfield.preset

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType
import cn.osmrpighead.envelog.ui.component.form.textfield.interactionfield.OutlinedNumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError
import cn.osmrpighead.envelog.ui.style.fieldPrefix
import cn.osmrpighead.envelog.ui.style.fieldSuffix
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.prefix
import cn.osmrpighead.envelog.ui.text.suffix
import java.math.RoundingMode

@Composable
fun OutlinedAmountField(
    amount: Long,
    onAmountChange: (Long) -> Unit,
    host: NumKeypadHostState,
    modifier: Modifier = Modifier,
    currency: CurrencyType? = null,
    nonPositive: Boolean = false,
    fieldId: Any = remember { Any() },
    format: Long.() -> String = { formatAsAmount(Formatter.AmountConcise) },
    scale: Int = 2,
    roundingMode: RoundingMode = RoundingMode.HALF_UP,
    textAlign: TextAlign = TextAlign.Start,
    onCancel: () -> Unit = {},
    label: (@Composable () -> Unit)? = null,
) {
    val consumer = remember {
        AmountConsumer(
            scale = scale,
            roundingMode = roundingMode,
            submit = onAmountChange,
            cancel = onCancel,
        )
    }
    val active = host.activeFieldId == fieldId
    val nonPositive = nonPositive ||
            (active && consumer.validationError == AmountValidationError.NonPositive)
    val error = nonPositive || (active && consumer.validationError != null)

    OutlinedNumKeypadField(
        value = amount.format(),
        host = host,
        consumer = consumer,
        fieldId = fieldId,
        isError = error,
        supportingText = if (nonPositive) {
            { Text(text = stringResource(R.string.common_msg_amount_positive)) }
        } else null,
        textStyle = LocalTextStyle.current.copy(textAlign = textAlign),
        label = label,
        prefix = currency?.let { { FieldPrefix(text = currency.prefix) } },
        suffix = currency?.let { { FieldSuffix(text = currency.suffix) } },
        modifier = modifier,
    )
}
