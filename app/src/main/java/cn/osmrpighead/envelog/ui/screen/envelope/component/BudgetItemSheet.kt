package cn.osmrpighead.envelog.ui.screen.envelope.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import cn.osmrpighead.envelog.ui.screen.envelope.model.BudgetItemSheetModel
import cn.osmrpighead.envelog.ui.text.Formatter
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.util.asAmount
import cn.osmrpighead.envelog.util.date.DatePeriod
import cn.osmrpighead.envelog.util.date.untilExclusive
import java.math.BigDecimal
import java.math.RoundingMode

class BudgetEditorNumKeypadConsumer(
    private val onAmountChange: (Long) -> Unit,
    private val showValidation: (Boolean) -> Unit,
    override val scale: Int = 0,
    override val roundingMode: RoundingMode = RoundingMode.HALF_UP,
) : NumKeypadConsumer {
    override fun validate(result: EvaluatedResult): Boolean = when (result) {
        is EvaluatedResult.Success -> result.value > BigDecimal(0)
        else -> false
    }.also(showValidation)

    override fun onConfirm(result: EvaluatedResult) =
        onAmountChange((result as EvaluatedResult.Success).value.asAmount(0, RoundingMode.HALF_UP))

    override fun onCancel() {}
}

@Composable
fun BudgetItemSheet(
    label: String,
    numKeypadHost: NumKeypadHostState,
    uiState: BudgetItemSheetModel,
    onPeriodChange: (DatePeriod) -> Unit,
    onAmountChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val budgetShouldBePositive = stringResource(R.string.budget_should_be_positive)
    var amountFieldState by remember { mutableStateOf(Pair<Boolean, String?>(false, null)) }
    val consumer = remember {
        BudgetEditorNumKeypadConsumer(
            onAmountChange,
            {
                amountFieldState = if (it) Pair(false, null)
                else Pair(true, budgetShouldBePositive)
            },
        )
    }

    val period = uiState.fields.period.value
        ?: (uiState.value.startDate untilExclusive uiState.value.endDate)

    LabeledBox(
        label = label,
        modifier = modifier,
    ) {
        DateRangePickerField(
            period = period.toClosed(),
            dialogBuilder = DateRangePickerDialogBuilder.Default(
                ConfirmEventHandler.Default { onPeriodChange(it.toHalfOpen()) },
            ),
            label = { Text(stringResource(R.string.budget_range)) },
        )
        NumKeypadField(
            value = (uiState.fields.amount.value ?: uiState.value.amount).formatAsAmount(Formatter.AmountInteger),
            host = numKeypadHost,
            consumer = consumer,
            label = { Text(stringResource(R.string.budget_amount)) },
            isError = amountFieldState.first,
            supportingText = {
                val text = amountFieldState.second ?: return@NumKeypadField
                Text(text = text)
            },
        )
    }
}
