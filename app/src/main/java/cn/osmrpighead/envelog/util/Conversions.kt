package cn.osmrpighead.envelog.util

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

fun BigDecimal.formatAmount(
    digits: Int = 2,
    sign: Boolean = false,
    shift: Int = 2,
    locale: Locale = Locale.getDefault(),
): String =
    (DecimalFormat.getNumberInstance(locale) as DecimalFormat).apply {
        minimumFractionDigits = digits
        maximumFractionDigits = digits
        isGroupingUsed = false
        if (sign) positivePrefix = "+"
    }.format(movePointLeft(shift))

fun Long.formatAmount(
    digits: Int = 2,
    sign: Boolean = false,
    shift: Int = 2,
    locale: Locale = Locale.getDefault(),
): String =
    BigDecimal(this).formatAmount(digits, sign, shift, locale)

fun Double.formatAmount(
    digits: Int = 2,
    sign: Boolean = false,
    shift: Int = 2,
    locale: Locale = Locale.getDefault(),
): String =
    BigDecimal(this).formatAmount(digits, sign, shift, locale)

fun Long.percent(denominator: Long, digits: Int = 0, locale: Locale = Locale.getDefault()): String =
    (this.toDouble() / denominator).formatAmount(digits, false, -2, locale)
