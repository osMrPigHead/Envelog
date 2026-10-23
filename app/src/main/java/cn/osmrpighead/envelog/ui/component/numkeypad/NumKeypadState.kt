package cn.osmrpighead.envelog.ui.component.numkeypad

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.ui.component.numkeypad.Operator.*
import java.math.BigDecimal
import java.math.RoundingMode

enum class Operator {
    Plus, Minus, Times, Divides
}


val Operator.text: String
    @Composable get() = when (this) {
        Plus -> stringResource(R.string.numkeypad_plus)
        Minus -> stringResource(R.string.numkeypad_minus)
        Times -> stringResource(R.string.numkeypad_times)
        Divides -> stringResource(R.string.numkeypad_divides)
    }

sealed interface EvaluatedResult {
    data object DividedByZero : EvaluatedResult
    data class Success(val value: BigDecimal) : EvaluatedResult
}

data class NumKeypadState(
    val scale: Int = 2,
    val roundingMode: RoundingMode = RoundingMode.HALF_UP,
    val fatal: Boolean = false,
    val leftOperand: String = "0",
    val rightOperand: String = "0",
    val operator: Operator? = null,
) {
    fun evaluate(): EvaluatedResult {
        return when (operator) {
            null -> EvaluatedResult.Success(leftOperand.toBigDecimal())
            Plus -> EvaluatedResult.Success(leftOperand.toBigDecimal().add(rightOperand.toBigDecimal()))
            Minus -> EvaluatedResult.Success(leftOperand.toBigDecimal().subtract(rightOperand.toBigDecimal()))
            Times -> EvaluatedResult.Success(leftOperand.toBigDecimal().multiply(rightOperand.toBigDecimal()))
            Divides -> if (rightOperand.toBigDecimal().compareTo(BigDecimal.ZERO) == 0) EvaluatedResult.DividedByZero
            else EvaluatedResult.Success(
                leftOperand.toBigDecimal().divide(rightOperand.toBigDecimal(), scale, roundingMode),
            )
        }
    }

    val text: String
        @Composable get() = if (operator == null) leftOperand
        else leftOperand + operator.text + rightOperand
}
