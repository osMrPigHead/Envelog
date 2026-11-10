package cn.osmrpighead.envelog.ui.component.form

import java.time.YearMonth

internal data class MonthPickerRange(
    val minYearMonth: YearMonth? = null,
    val maxYearMonth: YearMonth? = null,
) {
    init {
        require(minYearMonth == null || maxYearMonth == null || minYearMonth <= maxYearMonth) {
            "minYearMonth must not be after maxYearMonth"
        }
    }

    fun contains(month: YearMonth): Boolean {
        val min = minYearMonth
        val max = maxYearMonth
        return (min == null || month >= min) && (max == null || month <= max)
    }

    fun coerce(month: YearMonth): YearMonth {
        val min = minYearMonth
        val max = maxYearMonth
        return when {
            min != null && month < min -> min
            max != null && month > max -> max
            else -> month
        }
    }

    fun effectiveYearRange(reference: YearMonth, radius: Int = DEFAULT_YEAR_RADIUS): IntRange {
        require(radius >= 0) { "radius must not be negative" }
        val target = coerce(reference)
        val first = minYearMonth?.year ?: (target.year - radius)
        val last = maxYearMonth?.year ?: (target.year + radius)
        return first..last
    }

    fun monthsIn(year: Int): List<Int> {
        val first = minYearMonth?.takeIf { it.year == year }?.monthValue ?: 1
        val last = maxYearMonth?.takeIf { it.year == year }?.monthValue ?: 12
        return if (first > last) emptyList() else (first..last).toList()
    }

    private companion object {
        const val DEFAULT_YEAR_RADIUS = 100
    }
}
