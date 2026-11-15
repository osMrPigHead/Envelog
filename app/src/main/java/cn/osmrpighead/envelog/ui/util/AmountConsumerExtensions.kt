package cn.osmrpighead.envelog.ui.util

import cn.osmrpighead.envelog.ui.component.numkeypad.NumKeypadHostState
import cn.osmrpighead.envelog.ui.model.AmountConsumer
import cn.osmrpighead.envelog.ui.model.AmountValidationError

fun AmountConsumer.nonPositiveToError(error: Boolean, fieldId: Any, host: NumKeypadHostState): Pair<Boolean, Boolean> {
    val active = host.activeFieldId == fieldId
    return (error || (active && validationError == AmountValidationError.NonPositive)) to
            (error || (active && validationError != null))
}
