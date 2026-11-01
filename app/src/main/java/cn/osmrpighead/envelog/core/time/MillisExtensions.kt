package cn.osmrpighead.envelog.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

fun Long.toLocalDate(zone: ZoneId = ZoneOffset.UTC): LocalDate = Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
