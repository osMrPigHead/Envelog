package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon

private fun String.trimZero(): String {
    val s = trimStart('0')
    return when {
        s.startsWith('.') -> "0$s"
        s.isEmpty() -> "0"
        else -> s
    }
}

private fun numberInput(state: NumKeypadState, number: String): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = (state.leftOperand + number).trimZero())
    else state.copy(rightOperand = (state.rightOperand + number).trimZero())

private fun decimalPointInput(state: NumKeypadState): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = (state.leftOperand.replace(".", "") + ".").trimZero())
    else state.copy(rightOperand = (state.rightOperand.replace(".", "") + ".").trimZero())

private fun delete(state: NumKeypadState): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = state.leftOperand.dropLast(1).trimZero())
    else if (state.rightOperand.trimZero() != "0") state.copy(rightOperand = state.rightOperand.dropLast(1).trimZero())
    else state.copy(operator = null)

private fun operatorInput(state: NumKeypadState, operator: Operator): NumKeypadState =
    if (state.operator == null || state.rightOperand.trimZero() == "0")
        state.copy(operator = operator, rightOperand = "0")
    else when (val result = state.evaluate()) {
        is EvaluatedResult.Success -> state.copy(
            leftOperand = result.value.toPlainString(),
            operator = operator,
            rightOperand = "0",
        )

        EvaluatedResult.DividedByZero -> state.copy(fatal = true)
    }

private fun clear(state: NumKeypadState): NumKeypadState =
    if (state.fatal) state.copy(fatal = false)
    else if (state.operator == null || state.rightOperand.trimZero() == "0")
        state.copy(leftOperand = "0", operator = null, rightOperand = "0")
    else state.copy(rightOperand = "0")

@Composable
fun NumberButton(
    number: String,
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onStateChange(numberInput(state, number)) },
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.spacingMini)),
        enabled = !state.fatal,
        shape = MaterialTheme.shapes.large,
    ) { Text(text = number, style = MaterialTheme.typography.titleLarge) }
}

@Composable
fun DecimalPointButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onStateChange(decimalPointInput(state)) },
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.spacingMini)),
        enabled = !state.fatal,
        shape = MaterialTheme.shapes.large,
    ) { Text(text = ".", style = MaterialTheme.typography.titleLarge) }
}

@Composable
fun OperatorButton(
    operator: Operator,
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onStateChange(operatorInput(state, operator)) },
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.spacingMini)),
        enabled = !state.fatal,
        shape = MaterialTheme.shapes.large,
    ) { Text(text = operator.text, style = MaterialTheme.typography.titleLarge) }
}

@Composable
fun ClearButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onStateChange(clear(state)) },
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.spacingMini)),
        shape = MaterialTheme.shapes.large,
    ) {
        Text(
            text = stringResource(
                if (state.operator == null || state.rightOperand.trimZero() == "0")
                    R.string.numkeypad_allclear
                else R.string.numkeypad_clear,
            ),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
fun DeleteButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = { onStateChange(delete(state)) },
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.spacingMini)),
        enabled = !state.fatal,
        shape = MaterialTheme.shapes.large,
    ) {
        Icon(
            imageVector = Icon.Backspace,
            contentDescription = stringResource(R.string.numkeypad_backspace),
        )
    }
}

@Composable
fun NumKeypad(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    confirmEnabled: Boolean,
    onConfirm: (EvaluatedResult) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = when (val result = state.evaluate()) {
                is EvaluatedResult.Success -> "= ${result.value.toPlainString()}"
                EvaluatedResult.DividedByZero -> stringResource(R.string.numkeypad_divided_by_zero)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacingLarge), dimensionResource(R.dimen.spacingSmall)),
        )
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
        ) {
            ClearButton(state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Divides, state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Times, state, onStateChange, Modifier.weight(1f))
            DeleteButton(state, onStateChange, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
        ) {
            NumberButton("1", state, onStateChange, Modifier.weight(1f))
            NumberButton("2", state, onStateChange, Modifier.weight(1f))
            NumberButton("3", state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Plus, state, onStateChange, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .fillMaxWidth(),
        ) {
            NumberButton("4", state, onStateChange, Modifier.weight(1f))
            NumberButton("5", state, onStateChange, Modifier.weight(1f))
            NumberButton("6", state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Minus, state, onStateChange, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .weight(2f, fill = false)
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            Column(
                modifier = modifier.weight(3f),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                ) {
                    NumberButton("7", state, onStateChange, Modifier.weight(1f))
                    NumberButton("8", state, onStateChange, Modifier.weight(1f))
                    NumberButton("9", state, onStateChange, Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                ) {
                    NumberButton("0", state, onStateChange, Modifier.weight(2f))
                    DecimalPointButton(state, onStateChange, Modifier.weight(1f))
                }
            }
            Button(
                onClick = { onConfirm(state.evaluate()) },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(dimensionResource(R.dimen.spacingMini)),
                enabled = confirmEnabled,
                shape = MaterialTheme.shapes.large,
            ) { Text(text = stringResource(R.string.numkeypad_confirm), style = MaterialTheme.typography.titleLarge) }
        }
    }
}
