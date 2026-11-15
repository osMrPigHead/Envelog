package cn.osmrpighead.envelog.util

fun <T> List<T>.toSetIfNonDuplicates(): Set<T>? {
    val seen = HashSet<T>()
    return if (any { !seen.add(it) }) null else seen
}

fun <T> Set<T>.withElementIf(element: T, shouldInclude: Boolean): Set<T> =
    if (shouldInclude) this + element else this - element
