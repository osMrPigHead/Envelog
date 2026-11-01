package cn.osmrpighead.envelog.ui.component.numkeypad

import java.math.RoundingMode

interface NumKeypadConsumer {
    val scale: Int
    val roundingMode: RoundingMode

    fun validate(result: EvaluatedResult): Boolean
    fun onConfirm(result: EvaluatedResult)
    fun onCancel()
}
