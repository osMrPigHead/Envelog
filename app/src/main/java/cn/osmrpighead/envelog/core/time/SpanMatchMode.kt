package cn.osmrpighead.envelog.core.time

enum class SpanMatchMode {
    Overlaps,
    StartWithin,
    EndWithin,
    MajorityOverlapsFallbackStart,
    MajorityOverlapsFallbackEnd,
}