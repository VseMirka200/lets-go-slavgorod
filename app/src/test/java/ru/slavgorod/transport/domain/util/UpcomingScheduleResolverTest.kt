package ru.slavgorod.transport.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.slavgorod.transport.data.model.BusSchedule
import java.util.Calendar

class UpcomingScheduleResolverTest {

    @Test
    fun `findNextScheduleId returns nearest future departure`() {
        val schedules = listOf(
            schedule(id = "s1", time = "09:00"),
            schedule(id = "s2", time = "11:00"),
            schedule(id = "s3", time = "12:00")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(10, 30)
        )

        assertEquals("s2", result)
    }

    @Test
    fun `findNextScheduleId skips departure with one minute or less remaining`() {
        val schedules = listOf(
            schedule(id = "soon", time = "10:01"),
            schedule(id = "next", time = "10:30")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(10, 0)
        )

        assertEquals("next", result)
    }

    @Test
    fun `findNextScheduleId works for route one schedules like any other route`() {
        val schedules = listOf(
            schedule(id = "r1_early", time = "07:00", departurePoint = "Station"),
            schedule(id = "r1_late", time = "08:00", departurePoint = "Depot")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(7, 30)
        )

        assertEquals("r1_late", result)
    }

    @Test
    fun `findNextScheduleId returns earliest departure when day is over`() {
        val schedules = listOf(
            schedule(id = "s1", time = "06:00"),
            schedule(id = "s2", time = "07:00")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(23, 30)
        )

        assertEquals("s1", result)
    }

    @Test
    fun `findNextScheduleId ignores malformed time values`() {
        val schedules = listOf(
            schedule(id = "bad", time = "25:99"),
            schedule(id = "good", time = "11:00")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(10, 0)
        )

        assertEquals("good", result)
    }

    @Test
    fun `findNextScheduleId returns null when all times are malformed`() {
        val schedules = listOf(
            schedule(id = "bad_1", time = "invalid"),
            schedule(id = "bad_2", time = "99:99")
        )

        val result = UpcomingScheduleResolver.findNextScheduleId(
            schedules = schedules,
            currentTime = calendarAt(10, 0)
        )

        assertNull(result)
    }

    private fun calendarAt(hour: Int, minute: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun schedule(
        id: String,
        time: String,
        platform: String? = null,
        notes: String? = null,
        departurePoint: String = "Depot"
    ): BusSchedule {
        return BusSchedule(
            id = id,
            routeId = "1",
            stopName = "Test stop",
            departureTime = time,
            dayOfWeek = 2,
            platform = platform,
            notes = notes,
            departurePoint = departurePoint
        )
    }
}
