package ru.slavgorod.transport.data.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Календарь, определяющий дни выполнения одного рейса.
 *
 * Отсутствующее [operatingDays] означает, что рейс выполняется ежедневно.
 * Пустой набор означает рейсы только во включённые даты. Ограничения периода и
 * исключённые даты имеют приоритет даже над явно включёнными датами.
 */
data class ServiceCalendar(
    val operatingDays: Set<DayOfWeek>? = null,
    val validFrom: LocalDate? = null,
    val validUntil: LocalDate? = null,
    val includedDates: Set<LocalDate> = emptySet(),
    val excludedDates: Set<LocalDate> = emptySet()
) {

    /** Проверяет дату с учётом границ периода, исключений и дней недели. */
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
