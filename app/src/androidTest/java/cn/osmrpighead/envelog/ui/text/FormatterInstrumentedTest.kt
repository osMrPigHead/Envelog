package cn.osmrpighead.envelog.ui.text

import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.osmrpighead.envelog.core.number.Ratio
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsAmount
import cn.osmrpighead.envelog.ui.text.Formatter.formatAsPercent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Formatter 依赖 android.icu，只能在设备上验证。
 */
@RunWith(AndroidJUnit4::class)
class FormatterInstrumentedTest {
    @Test
    fun amountFormatters_applyFixedAndConcisePrecision() {
        assertEquals("1,234.56", 123_456L.formatAsAmount(Formatter.AmountStandard))
        assertEquals("1,000", 100_000L.formatAsAmount(Formatter.AmountConcise))
        assertEquals("1,234", 123_400L.formatAsAmount(Formatter.AmountInteger))
    }

    @Test
    fun signedAmountFormatters_markOnlyNonZeroValues() {
        assertEquals("+123.45", 12_345L.formatAsAmount(Formatter.AmountSignedStandard))
        assertEquals("-123.45", (-12_345L).formatAsAmount(Formatter.AmountSignedStandard))
        assertEquals("0.00", 0L.formatAsAmount(Formatter.AmountSignedStandard))
    }

    @Test
    fun percentFormatters_formatValidRatiosWithoutTruncation() {
        assertEquals("25.00%", Ratio.of(1, 4).formatAsPercent(Formatter.PercentStandard))
        assertEquals("25%", Ratio.of(1, 4).formatAsPercent(Formatter.PercentInteger))
        assertEquals("150.00%", Ratio.of(3, 2).formatAsPercent(Formatter.PercentStandard))
        assertEquals("150%", Ratio.of(3, 2).formatAsPercent(Formatter.PercentInteger))
        assertEquals("-50.00%", Ratio.of(-1, 2).formatAsPercent(Formatter.PercentStandard))
        assertEquals("-50%", Ratio.of(-1, 2).formatAsPercent(Formatter.PercentInteger))
        assertNull(Ratio.of(1, 0).formatAsPercent(Formatter.PercentStandard))
        assertNull(Ratio.of(1, 0).formatAsPercent(Formatter.PercentInteger))
    }
}
