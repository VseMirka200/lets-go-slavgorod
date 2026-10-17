package ru.slavgorod.transport.data.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Calendar that determines when one scheduled trip is in service.
 *
 * A missing [operatingDays] means that the trip is daily. An empty set means
 * that it only runs on explicitly included dates.
 */
data class ServiceCalendar(
    val operatingDays: Set<DayOfWeek>? = null,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val includedDates: Set<LocalDate> = emptySet(),
    val excludedDates: Set<LocalDate> = emptySet()
) {

    fun runsOn(date: LocalDate): Boolean {
        if (date in excludedDates) return false
        if (validFrom != null && date < validFrom) return false
        if (validUntil != null && date > validUntil) return false
        if (date in includedDates) return true
        return operatingDays?.contains(date.dayOfWeek) ?: true
    }

    companion object {
        val Daily = ServiceCalendar()
    }
}
