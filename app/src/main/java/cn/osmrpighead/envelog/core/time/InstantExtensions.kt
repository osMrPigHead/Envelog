package cn.osmrpighead.envelog.core.time

import cn.osmrpighead.envelog.core.locale.AppLocale
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun Instant.toLocalDateTime(zoneId: ZoneId): LocalDateTime = atZone(zoneId).toLocalDateTime()
fun Instant.toLocalDate(zoneId: ZoneId): LocalDate = atZone(zoneId).toLocalDate()

fun Instant.format(
    zoneId: ZoneId,
    style: FormatStyle = FormatStyle.SHORT,
    locale: Locale = AppLocale.current(),
): String = atZone(zoneId).format(DateTimeFormatter.ofLocalizedDateTime(style).withLocale(locale))
