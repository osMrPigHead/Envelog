package cn.osmrpighead.envelog.ui.text

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cn.osmrpighead.envelog.R
import cn.osmrpighead.envelog.core.number.CurrencyType

val CurrencyType.prefix: String
    @Composable get() = when (this) {
        CurrencyType.CNY -> stringResource(R.string.currency_cny_prefix)
    }

val CurrencyType.suffix: String
    @Composable get() = when (this) {
        CurrencyType.CNY -> stringResource(R.string.currency_cny_suffix)
    }

@Composable
fun String.withCurrency(currency: CurrencyType): String =
    stringResource(R.string.currency_format, this, currency.prefix, currency.suffix)
