package cn.osmrpighead.envelog.util

fun String.trimAndCheck(throwable: Throwable): String {
    val trimmed = trim()
    if (trimmed.isBlank()) throw throwable
    return trimmed
}
