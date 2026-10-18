package cn.osmrpighead.envelog.ui.text

import android.icu.number.FormattedNumber
import android.icu.number.LocalizedNumberFormatter
import android.icu.number.NumberFormatter
import android.icu.number.NumberFormatterSettings
import android.icu.number.Precision
import android.icu.number.UnlocalizedNumberFormatter
import android.icu.util.MeasureUnit
import android.icu.util.ULocale
import cn.osmrpighead.envelog.common.locale.AppLocale
import cn.osmrpighead.envelog.util.Ratio
import cn.osmrpighead.envelog.util.clamp
import java.math.BigDecimal
import java.util.Locale

object Formatter {
    sealed interface PercentFormatPolicy {
        val formatter: NumberFormatterSettings<*>
        val clampStep: BigDecimal

        data class Unlocalized(
            override val formatter: UnlocalizedNumberFormatter,
            override val clampStep: BigDecimal,
        ) : PercentFormatPolicy {
            fun locale(locale: Locale): Localized = Localized(formatter.locale(locale), clampStep)
            fun locale(locale: ULocale): Localized = Localized(formatter.locale(locale), clampStep)
        }

        data class Localized(
            override val formatter: LocalizedNumberFormatter,
            override val clampStep: BigDecimal,
        ) : PercentFormatPolicy
    }

    val AmountStandard: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.fixedFraction(2))
    val AmountConcise: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.maxFraction(2))
    val AmountInteger: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.integer())
    val AmountSignedStandard: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.fixedFraction(2))
            .sign(NumberFormatter.SignDisplay.EXCEPT_ZERO)
    val AmountSignedConcise: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.maxFraction(2))
            .sign(NumberFormatter.SignDisplay.EXCEPT_ZERO)
    val AmountSignedInteger: UnlocalizedNumberFormatter =
        NumberFormatter.with()
            .precision(Precision.integer())
            .sign(NumberFormatter.SignDisplay.EXCEPT_ZERO)
    val PercentStandard = PercentFormatPolicy.Unlocalized(
        formatter = NumberFormatter.with()
            .unit(MeasureUnit.PERCENT)
            .precision(Precision.fixedFraction(2)),
        clampStep = BigDecimal("0.0001"),
    )
    val PercentInteger = PercentFormatPolicy.Unlocalized(
        formatter = NumberFormatter.with()
            .unit(MeasureUnit.PERCENT)
            .precision(Precision.integer()),
        clampStep = BigDecimal("0.01"),
    )

    fun Long.formattedAmount(formatter: LocalizedNumberFormatter): FormattedNumber =
        formatter.format(BigDecimal.valueOf(this, 2))

    fun Long.formattedAmount(formatter: UnlocalizedNumberFormatter): FormattedNumber =
        formattedAmount(formatter.locale(AppLocale.current()))

    fun Long.formatAsAmount(formatter: LocalizedNumberFormatter): String =
        formattedAmount(formatter).toString()

    fun Long.formatAsAmount(formatter: UnlocalizedNumberFormatter): String =
        formatAsAmount(formatter.locale(AppLocale.current()))

    fun Ratio.formatAsPercent(policy: PercentFormatPolicy.Localized): String? =
        if (!isValid) null
        else policy.formatter.format(
            ratio.clamp(
                min = BigDecimal.ZERO,
                max = BigDecimal.ONE,
                step = policy.clampStep,
            ).movePointRight(2),
        ).toString()

    fun Ratio.formatAsPercent(policy: PercentFormatPolicy.Unlocalized): String? =
        formatAsPercent(policy.locale(AppLocale.current()))
}
