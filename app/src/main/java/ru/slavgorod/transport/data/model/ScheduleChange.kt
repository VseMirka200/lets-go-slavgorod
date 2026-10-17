package ru.slavgorod.transport.data.model

import java.time.LocalDate

data class ScheduleChange(
    val id: String,
    val routeIds: Set<String> = emptySet(),
    val title: String,
    val text: String,
    val validFrom: LocalDate,
    val validUntil: LocalDate? = null
) {
    fun isActiveOn(date: LocalDate): Boolean = date >= validFrom && (validUntil == null || date <= validUntil)
    fun appliesTo(routeId: String): Boolean = routeIds.isEmpty() || routeId in routeIds
}
