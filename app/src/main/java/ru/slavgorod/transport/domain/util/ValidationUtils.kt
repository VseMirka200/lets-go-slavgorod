package ru.slavgorod.transport.domain.util

import timber.log.Timber
import java.net.URL
import java.util.Calendar

object ValidationUtils {

    private val colorArgbRegex = Regex("^#[0-9A-Fa-f]{8}$")
    private val colorRgbRegex = Regex("^#[0-9A-Fa-f]{6}$")
    private val allowedProtocols = setOf("http", "https")

    fun isValidTime(time: String): Boolean {
        if (time.isBlank()) return false

        val timeParts = time.split(":")
        if (timeParts.size != 2) return false

        return try {
            val hour = timeParts[0].trim().toInt()
            val minute = timeParts[1].trim().toInt()
            hour in 0..23 && minute in 0..59
        } catch (_: NumberFormatException) {
            false
        }
    }

    fun isValidDayOfWeek(dayOfWeek: Int): Boolean {
        val isValid = dayOfWeek == 0 || dayOfWeek in Calendar.SUNDAY..Calendar.SATURDAY
        if (!isValid) {
            Timber.e("Invalid day of week: %s", dayOfWeek)
        }
        return isValid
    }

    fun isValidRouteId(routeId: String?): Boolean = hasMeaningfulText(routeId)

    fun isValidRouteName(routeName: String?): Boolean = hasTrimmedLengthAtLeast(routeName, 3)

    fun isValidRouteNumber(routeNumber: String?): Boolean = hasMeaningfulText(routeNumber)

    fun sanitizeString(input: String?): String {
        return input.trimToNullText().orEmpty()
    }

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false

        return try {
            URL(url.trim()).protocol in allowedProtocols
        } catch (_: Exception) {
            false
        }
    }

    fun normalizeColor(color: String?): String? {
        if (color.isNullOrBlank()) return null

        val trimmed = color.trim()
        return when {
            colorArgbRegex.matches(trimmed) -> trimmed.uppercase()
            colorRgbRegex.matches(trimmed) -> "#FF${trimmed.substring(1).uppercase()}"
            else -> null
        }
    }

    fun isValidColor(color: String?): Boolean = normalizeColor(color) != null

    internal fun hasMeaningfulText(value: String?): Boolean {
        return value.trimToNullText() != null
    }

    internal fun hasTrimmedLengthAtLeast(value: String?, minLength: Int): Boolean {
        return (value.trimToNullText()?.length ?: 0) >= minLength
    }
}

private fun String?.trimToNullText(): String? {
    return this?.trim()?.takeIf { it.isNotBlank() }
}
