package cn.osmrpighead.envelog.core.number

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class Ratio(
    val ratio: BigDecimal,
    val isValid: Boolean,
) {
    companion object {
        fun of(numerator: BigDecimal, denominator: BigDecimal, mc: MathContext = MathContext.DECIMAL32): Ratio =
            if (denominator == BigDecimal.ZERO) Ratio(BigDecimal.ZERO, false)
            else Ratio(numerator.divide(denominator, mc), true)

        fun of(numerator: Long, denominator: Long, mc: MathContext = MathContext.DECIMAL32): Ratio =
            of(BigDecimal(numerator), BigDecimal(denominator), mc)
    }
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

// TODO: Currency
fun BigDecimal.asAmount(scale: Int, roundingMode: RoundingMode): Long =
    setScale(scale, roundingMode).movePointRight(2).toLong()

fun Long.ratioOf(denominator: Long): Ratio = Ratio.of(this, denominator)
