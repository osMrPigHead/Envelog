package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.icon.Icon
import cn.osmrpighead.envelog.ui.style.radialFilledWeight
import cn.osmrpighead.envelog.ui.theme.ButtonColor
import cn.osmrpighead.envelog.ui.theme.MotionTokens

private fun String.normalize(): String {
    val s = trimStart('0').run {
        when {
            startsWith('.') -> "0$this"
            isEmpty() -> "0"
            else -> this
        }
    }
    return if (s.toBigDecimalOrNull() == null) "0" else s
}

private fun numberInput(state: NumKeypadState, number: String): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = (state.leftOperand + number).normalize())
    else state.copy(rightOperand = (state.rightOperand + number).normalize())

private fun decimalPointInput(state: NumKeypadState): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = (state.leftOperand.replace(".", "") + ".").normalize())
    else state.copy(rightOperand = (state.rightOperand.replace(".", "") + ".").normalize())

private fun delete(state: NumKeypadState): NumKeypadState =
    if (state.operator == null) state.copy(leftOperand = state.leftOperand.dropLast(1).normalize())
    else if (state.rightOperand.normalize() != "0") state.copy(
        rightOperand = state.rightOperand.dropLast(1).normalize(),
    )
    else state.copy(operator = null)

private fun operatorInput(state: NumKeypadState, operator: Operator): NumKeypadState =
    if (state.operator == null || state.rightOperand.normalize() == "0")
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
    else if (state.operator == null || state.rightOperand.normalize() == "0")
        state.copy(leftOperand = "0", operator = null, rightOperand = "0")
    else state.copy(rightOperand = "0")

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    colors: ButtonColors = ButtonColor.textNormal,
    content: @Composable RowScope.() -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .height(IntrinsicSize.Max)
            .padding(dimensionResource(R.dimen.space2)),
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = colors,
        content = content,
    )
}

@Composable
fun NumberButton(
    number: String,
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) = KeypadButton(
    onClick = { onStateChange(numberInput(state, number)) },
    enabled = state.enabled && !state.fatal,
    modifier = modifier,
) { Text(text = number, style = MaterialTheme.typography.titleLarge) }

@Composable
fun DecimalPointButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) = KeypadButton(
    onClick = { onStateChange(decimalPointInput(state)) },
    enabled = state.enabled && !state.fatal,
    modifier = modifier,
) { Text(text = ".", style = MaterialTheme.typography.titleLarge) }

@Composable
fun OperatorButton(
    operator: Operator,
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) = KeypadButton(
    onClick = { onStateChange(operatorInput(state, operator)) },
    enabled = state.enabled && !state.fatal,
    modifier = modifier,
    colors = ButtonDefaults.textButtonColors(),
) { Text(text = operator.text, style = MaterialTheme.typography.titleLarge) }

@Composable
fun ClearButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) = KeypadButton(
    onClick = { onStateChange(clear(state)) },
    enabled = state.enabled,
    modifier = modifier,
    colors = ButtonColor.textDanger,
) {
    Text(
        text = stringResource(
            if (state.operator == null || state.rightOperand.normalize() == "0") R.string.numkeypad_action_all_clear
            else R.string.numkeypad_action_clear,
        ),
        style = MaterialTheme.typography.titleLarge,
    )
}

@Composable
fun DeleteButton(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    modifier: Modifier = Modifier,
) = KeypadButton(
    onClick = { onStateChange(delete(state)) },
    enabled = state.enabled && !state.fatal,
    modifier = modifier,
    colors = ButtonColor.textDanger,
) { Icon(imageVector = Icon.Backspace, contentDescription = stringResource(R.string.numkeypad_action_backspace)) }

@Composable
fun NumKeypad(
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = state.evaluate()
    Column(modifier = modifier.padding(horizontal = dimensionResource(R.dimen.space20))) {
        Text(
            text = when (result) {
                is EvaluatedResult.Success -> "= ${result.value.toPlainString()}"
                EvaluatedResult.DividedByZero -> stringResource(R.string.numkeypad_msg_divided_by_zero)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.space24),
                    vertical = dimensionResource(R.dimen.space8),
                ),
        )
        Row(modifier = Modifier.radialFilledWeight(1f, fill = false)) {
            ClearButton(state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Divides, state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Times, state, onStateChange, Modifier.weight(1f))
            DeleteButton(state, onStateChange, Modifier.weight(1f))
        }
        Row(modifier = Modifier.radialFilledWeight(1f, fill = false)) {
            NumberButton("1", state, onStateChange, Modifier.weight(1f))
            NumberButton("2", state, onStateChange, Modifier.weight(1f))
            NumberButton("3", state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Plus, state, onStateChange, Modifier.weight(1f))
        }
        Row(modifier = Modifier.radialFilledWeight(1f, fill = false)) {
            NumberButton("4", state, onStateChange, Modifier.weight(1f))
            NumberButton("5", state, onStateChange, Modifier.weight(1f))
            NumberButton("6", state, onStateChange, Modifier.weight(1f))
            OperatorButton(Operator.Minus, state, onStateChange, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier
                .radialFilledWeight(2f, fill = false)
                .height(IntrinsicSize.Max),
        ) {
            Column(modifier = Modifier.weight(3f)) {
                Row(modifier = Modifier.radialFilledWeight(1f, fill = false)) {
                    NumberButton("7", state, onStateChange, Modifier.weight(1f))
                    NumberButton("8", state, onStateChange, Modifier.weight(1f))
                    NumberButton("9", state, onStateChange, Modifier.weight(1f))
                }
                Row(modifier = Modifier.radialFilledWeight(1f, fill = false)) {
                    KeypadButton(
                        onClick = onCancel,
                        enabled = state.enabled,
                        modifier = Modifier.weight(1f),
                        colors = ButtonColor.textDanger,
                    ) {
                        Icon(
                            imageVector = Icon.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.common_action_cancel),
                        )
                    }
                    NumberButton("0", state, onStateChange, Modifier.weight(1f))
                    DecimalPointButton(state, onStateChange, Modifier.weight(1f))
                }
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.space2))
                    .radialFilledWeight(1f),
                enabled = state.enabled && confirmEnabled,
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(R.string.common_action_finish),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
fun BottomAnimatedNumKeypad(
    visibleState: MutableTransitionState<Boolean>,
    state: NumKeypadState,
    onStateChange: (NumKeypadState) -> Unit,
    confirmEnabled: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    surfaceBackground: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    AnimatedVisibility(
        visibleState = visibleState,
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
            color = surfaceBackground,
        ) {
            NumKeypad(
                state, onStateChange, confirmEnabled, onConfirm, onCancel,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
