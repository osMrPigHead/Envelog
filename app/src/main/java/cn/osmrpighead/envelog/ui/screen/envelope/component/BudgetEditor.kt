package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.LabeledBox
import cn.osmrpighead.envelog.ui.component.form.ConfirmEventHandler
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerDialogBuilder
import cn.osmrpighead.envelog.ui.component.form.DateRangePickerField
import cn.osmrpighead.envelog.ui.component.form.NumKeypadField
import cn.osmrpighead.envelog.ui.component.numkeypad.EvaluatedResult
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.util.asAmount
import cn.osmrpighead.envelog.util.date.DatePeriod
import java.math.BigDecimal
import java.math.RoundingMode

class BudgetEditorNumKeypadConsumer(
    private val onAmountChange: (Long) -> Unit,
    override val scale: Int = 0,
    override val roundingMode: RoundingMode = RoundingMode.HALF_UP,
) : NumKeypadConsumer {
    override fun validate(result: EvaluatedResult): Boolean = when (result) {
        is EvaluatedResult.Success -> result.value > BigDecimal(0)
        else -> false
    }

    override fun onConfirm(result: EvaluatedResult) =
        onAmountChange((result as EvaluatedResult.Success).value.asAmount(0, RoundingMode.HALF_UP))

    override fun onCancel() {}
}

@Composable
fun BudgetEditor(
    label: String,
    numKeypadHost: NumKeypadHostState,
    period: DatePeriod,
    onPeriodChange: (DatePeriod) -> Unit,
    amount: Long,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val consumer = remember { BudgetEditorNumKeypadConsumer(onAmountChange) }

    LabeledBox(
        label = label,
        modifier = modifier,
    ) {
        DateRangePickerField(
            period = period,
            dialogBuilder = DateRangePickerDialogBuilder.Default(ConfirmEventHandler.Default(onPeriodChange)),
            label = { Text(stringResource(R.string.budget_range)) },
        )
        NumKeypadField(
            value = amount.formatAsAmount(Formatter.AmountInteger),
            host = numKeypadHost,
            consumer = consumer,
            label = { Text(stringResource(R.string.budget_amount)) },
        )
    }
}
