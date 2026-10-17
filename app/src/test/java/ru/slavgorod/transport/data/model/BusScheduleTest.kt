package ru.slavgorod.transport.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusScheduleTest {

    @Test
    fun `isValid should return true for valid schedule`() {
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Slavgorod (Market)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Slavgorod (Market)"
        )

        assertTrue(schedule.isValid())
    }

    @Test
    fun `isValid should return false for invalid schedule ID`() {
        val schedule = BusSchedule(
            id = "",
            routeId = "route_1",
            stopName = "Slavgorod (Market)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Slavgorod (Market)"
        )

        assertFalse(schedule.isValid())
    }

    @Test
    fun `default values should be set correctly`() {
        val schedule = BusSchedule(
            id = "schedule_1",
            routeId = "route_1",
            stopName = "Slavgorod (Market)",
            departureTime = "08:00",
            dayOfWeek = 1,
            departurePoint = "Slavgorod (Market)"
        )

        assertFalse(schedule.isWeekend)
        assertNull(schedule.notes)
    }
}
