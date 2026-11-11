package cn.osmrpighead.envelog.ui.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.osmrpighead.envelog.core.number.asAmount
import cn.osmrpighead.envelog.ui.component.numkeypad.EvaluatedResult
import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadConsumer
import java.math.BigDecimal
import java.math.RoundingMode

sealed interface AmountValidationError {
    data class MathError(val error: EvaluatedResult.Error) : AmountValidationError
    data object NonPositive : AmountValidationError
}

class AmountConsumer(
    override val scale: Int,
    override val roundingMode: RoundingMode,
    private val submit: (Long) -> Unit,
    private val cancel: () -> Unit,
) : NumKeypadConsumer {
    var validationError by mutableStateOf<AmountValidationError?>(null)
        private set

    private fun fail(error: AmountValidationError): Boolean {
        validationError = error
        return false
    }

    private fun success(): Boolean {
        validationError = null
        return true
    }

    private fun successIf(success: Boolean, failAction: () -> Boolean): Boolean =
        if (success) success()
        else failAction()

    override fun validate(result: EvaluatedResult): Boolean = when (result) {
        is EvaluatedResult.Error -> fail(AmountValidationError.MathError(result))
        is EvaluatedResult.Success ->
            successIf(result.value > BigDecimal.ZERO) { fail(AmountValidationError.NonPositive) }
    }

    override fun onConfirm(result: EvaluatedResult) {
        require(result is EvaluatedResult.Success && result.value > BigDecimal.ZERO)
        submit(result.value.asAmount(scale, roundingMode))
    }

    override fun onCancel() {
        cancel()
    }
}