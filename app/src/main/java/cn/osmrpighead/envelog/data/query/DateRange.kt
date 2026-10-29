package cn.osmrpighead.envelog.data.query

import cn.osmrpighead.envelog.data.query.DateRangeRule.*
import cn.osmrpighead.envelog.core.time.DatePeriod
import cn.osmrpighead.envelog.core.time.untilExclusive
import java.time.LocalDate

data class DateRange(
    val period: DatePeriod,
    val rule: DateRangeRule,
) {
    constructor(start: LocalDate, end: LocalDate, rule: DateRangeRule) : this(start untilExclusive end, rule)
}

enum class DateRangeRule {
    StartInRange, EndInRange, Overlap, MajorityOverlapFallbackStart, MajorityOverlapFallbackEnd
}

fun DateRange.judge(range: DatePeriod): Boolean = when (rule) {
    StartInRange -> range.start in period
    EndInRange -> range.endExclusive in period
    Overlap -> range overlap period
    MajorityOverlapFallbackStart -> period.isMajorityOf(range) { it.start in period }
    MajorityOverlapFallbackEnd -> period.isMajorityOf(range) { it.endExclusive in period }
}

operator fun DateRange.contains(date: LocalDate): Boolean = date in period
