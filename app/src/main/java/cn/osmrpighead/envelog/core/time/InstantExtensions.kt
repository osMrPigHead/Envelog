package cn.osmrpighead.envelog.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

fun Instant.toLocalDateTime(zoneId: ZoneId): LocalDateTime = atZone(zoneId).toLocalDateTime()
fun Instant.toLocalDate(zoneId: ZoneId): LocalDate = atZone(zoneId).toLocalDate()
