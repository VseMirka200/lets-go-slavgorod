package ru.slavgorod.transport.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.slavgorod.transport.data.model.BusSchedule
import java.util.Calendar

class ScheduleScreenTest {

    @Test
    fun `buildScheduleScreenData preserves distinct departure points`() {
        val schedules = listOf(
            schedule(id = "s1", departurePoint = "Station", time = "07:00"),
            schedule(id = "s2", departurePoint = "1", time = "07:30"),
            schedule(id = "s3", departurePoint = "Depot", time = "08:00"),
            schedule(id = "s4", departurePoint = "2", time = "08:30")
        )

        val result = buildScheduleScreenData(
            allSchedules = schedules,
            currentTime = calendarAt0600()
        )

        assertEquals(4, result.points.size)
        assertEquals("Station", result.points.first().name)
        assertEquals("2", result.points.last().name)
        assertEquals(4, result.totalSchedules)
    }

    @Test
    fun `buildScheduleScreenData keeps numeric departure points as separate sections`() {
        val schedules = listOf(
            schedule(id = "s1", departurePoint = "Depot", time = "07:20"),
            schedule(id = "s2", departurePoint = "12", time = "20:20"),
            schedule(id = "s3", departurePoint = "12", time = "21:20")
        )

        val result = buildScheduleScreenData(
            allSchedules = schedules,
            currentTime = calendarAt0600()
        )

        assertEquals(listOf("Depot", "12"), result.points.map { it.name })
        assertEquals(listOf("s1"), result.points[0].schedules.map { it.id })
        assertEquals(listOf("s2", "s3"), result.points[1].schedules.map { it.id })
    }

    private fun schedule(
        id: String,
        departurePoint: String,
        time: String
    ): BusSchedule {
        return BusSchedule(
            id = id,
            routeId = "1",
            stopName = departurePoint,
            departureTime = time,
            dayOfWeek = 2,
            departurePoint = departurePoint
        )
    }

    private fun calendarAt0600(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
}
