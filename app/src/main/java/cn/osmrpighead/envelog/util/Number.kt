package cn.osmrpighead.envelog.util

import java.math.BigDecimal
import java.math.RoundingMode

data class Ratio(
    val ratio: BigDecimal,
    val isValid: Boolean,
) {
    constructor(ratio: String, isValid: Boolean) : this(ratio.toBigDecimal(), isValid)
}

fun BigDecimal.clamp(min: BigDecimal, max: BigDecimal, step: BigDecimal): BigDecimal {
    val minWithStep = min.add(step)
    val maxWithStep = max.subtract(step)
    return when {
        this.compareTo(min) == 0 -> min
        this.compareTo(max) == 0 -> max
        this > min && this < minWithStep -> minWithStep
        this > maxWithStep && this < max -> maxWithStep
        else -> this
    }
}

fun Long.percent(denominator: Long): Ratio {
    return if (denominator == 0L) Ratio("0", false)
    else Ratio(BigDecimal(this).divide(BigDecimal(denominator), 4, RoundingMode.HALF_EVEN), true)
}
