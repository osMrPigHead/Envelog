package cn.osmrpighead.envelog.util

import java.util.UUID

fun String.trimAndCheck(throwable: Throwable): String {
    val trimmed = trim()
    if (trimmed.isBlank()) throw throwable
    return trimmed
}

interface HasId {
    val id: UUID
}