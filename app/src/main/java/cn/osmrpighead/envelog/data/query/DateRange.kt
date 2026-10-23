package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.query.DateRangeRule.*
import cn.osmrpighead.envelog.util.date.DatePeriod
import cn.osmrpighead.envelog.util.date.contains
import cn.osmrpighead.envelog.util.date.isMajorityOf
import cn.osmrpighead.envelog.util.date.overlap
import java.time.LocalDate

data class DateRange(
    val period: DatePeriod,
    val rule: DateRangeRule,
) {
    constructor(start: LocalDate, end: LocalDate, rule: DateRangeRule) : this(start to end, rule)
}

enum class DateRangeRule {
    StartInRange, EndInRange, Overlap, MajorityOverlapFallbackStart, MajorityOverlapFallbackEnd
}

fun DateRange.judge(range: DatePeriod): Boolean = when (rule) {
    StartInRange -> range.first in period
    EndInRange -> range.second in period
    Overlap -> range overlap period
    MajorityOverlapFallbackStart -> period.isMajorityOf(range) { it.first in period }
    MajorityOverlapFallbackEnd -> period.isMajorityOf(range) { it.second in period }
}

operator fun DateRange.contains(date: LocalDate): Boolean = date in period
