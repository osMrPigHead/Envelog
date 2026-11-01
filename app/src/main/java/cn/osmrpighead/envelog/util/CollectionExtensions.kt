package cn.osmrpighead.envelog.util

fun <T> List<T>.toSetIfNonDuplicates(): Set<T>? {
    val seen = HashSet<T>()
    return if (any { !seen.add(it) }) null else seen
}