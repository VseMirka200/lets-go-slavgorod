package ru.slavgorod.transport.domain.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeFormatterUtils {

    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun formatFullDateTime(timestampMillis: Long, locale: Locale = Locale.getDefault()): String {
        return formatter("dd.MM.yyyy HH:mm", locale).format(Instant.ofEpochMilli(timestampMillis))
    }

    fun formatShortDateTime(timestampMillis: Long, locale: Locale = Locale.getDefault()): String {
        return formatter("dd.MM HH:mm", locale).format(Instant.ofEpochMilli(timestampMillis))
    }

    fun formatLogTimestamp(timestampMillis: Long, locale: Locale = Locale.getDefault()): String {
        return formatter("yyyy-MM-dd HH:mm:ss.SSS", locale).format(
            Instant.ofEpochMilli(
                timestampMillis
            )
        )
    }

    private fun formatter(pattern: String, locale: Locale): DateTimeFormatter {
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(zoneId)
    }

}
