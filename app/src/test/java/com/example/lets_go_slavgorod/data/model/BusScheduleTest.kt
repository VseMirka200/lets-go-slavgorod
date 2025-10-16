package com.example.lets_go_slavgorod.data.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Тесты для модели BusSchedule
 */
class BusScheduleTest {

    @Test
    fun `isValid should return true for valid schedule`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 1, // Monday
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertTrue(isValid)
    }

    @Test
    fun `isValid should return false for invalid schedule ID`() {
        // Given
        val schedule = BusSchedule(
            id = "", // Invalid empty ID
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid route ID`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "", // Invalid empty route ID
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid stop name`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "", // Invalid empty stop name
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid departure time`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "invalid_time", // Invalid time format
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `isValid should return false for invalid day of week`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 8, // Invalid day of week (should be 1-7)
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val isValid = schedule.isValid()

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `default values should be set correctly`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // Then
        assertFalse(schedule.isWeekend)
        assertNull(schedule.notes)
    }

    @Test
    fun `sanitized should return valid schedule with sanitized data`() {
        // Given
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Славгород (Рынок)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Славгород (Рынок)"
        )

        // When
        val sanitized = schedule.sanitized()

        // Then
        assertNotNull(sanitized)
        assertTrue(sanitized.isValid())
    }
}
