package ru.slavgorod.transport.ui.components.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.ui.model.DeparturePointSchedules

class ScheduleListStateTest {

    @Test
    fun `display state exposes upcoming entries for departure points`() {
        val displayState = buildScheduleListDisplayState(
            departurePoints = listOf(
                departurePoint(
                    name = "Center",
                    schedules = listOf(schedule("1", "08:00"), schedule("2", "09:00")),
                    nextUpcomingId = "1"
                )
            ),
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = null,
                selectedDeparturePointId = null
            ),
            scheduleExtraLabelProvider = null
        )

        assertEquals(1, displayState.upcomingEntries.size)
        assertEquals("Center", displayState.upcomingEntries.first().title)
        assertEquals("08:00", displayState.upcomingEntries.first().schedule.departureTime)
        assertEquals("09:00", displayState.upcomingEntries.first().nextDepartureTime)
    }

    @Test
    fun `display state filters schedules by description label`() {
        val displayState = buildScheduleListDisplayState(
            departurePoints = listOf(
                departurePoint(
                    name = "Center",
                    schedules = listOf(
                        schedule("1", "08:00", variant = "weekday"),
                        schedule("2", "09:00", variant = "weekend")
                    )
                )
            ),
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = "weekday",
                selectedDeparturePointId = null
            ),
            scheduleExtraLabelProvider = { schedule -> schedule.variant }
        )

        assertTrue(displayState.hasFilterControls)
        assertEquals(listOf("weekday", "weekend"), displayState.descriptionFilterLabels)
        assertEquals(1, displayState.visibleSchedulesCount)
        assertEquals("1", displayState.visibleSections.single().schedules.single().id)
    }

    @Test
    fun `display state filters schedules by departure point`() {
        val displayState = buildScheduleListDisplayState(
            departurePoints = listOf(
                departurePoint("Center", listOf(schedule("1", "08:00"))),
                departurePoint("Station", listOf(schedule("2", "09:00")))
            ),
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = null,
                selectedDeparturePointId = "2"
            ),
            scheduleExtraLabelProvider = null
        )

        assertEquals(1, displayState.visibleSections.size)
        assertEquals("Station", displayState.visibleSections.single().title)
        assertEquals("2", displayState.visibleSections.single().schedules.single().id)
    }

    @Test
    fun `display state reports empty visible sections for unmatched description`() {
        val displayState = buildScheduleListDisplayState(
            departurePoints = listOf(
                departurePoint("Center", listOf(schedule("1", "08:00", variant = "weekday")))
            ),
            filterState = ScheduleFilterState(
                selectedDescriptionLabel = "night",
                selectedDeparturePointId = null
            ),
            scheduleExtraLabelProvider = { schedule -> schedule.variant }
        )

        assertFalse(displayState.allSchedules.isEmpty())
        assertTrue(displayState.visibleSections.isEmpty())
        assertEquals(0, displayState.visibleSchedulesCount)
    }

    private fun departurePoint(
        name: String,
        schedules: List<BusSchedule>,
        nextUpcomingId: String? = null
    ): DeparturePointSchedules {
        return DeparturePointSchedules(
            name = name,
            schedules = schedules,
            nextUpcomingId = nextUpcomingId
        )
    }

    private fun schedule(
        id: String,
        departureTime: String,
        variant: String? = null
    ): BusSchedule {
        return BusSchedule(
            id = id,
            routeId = "route",
            stopName = "Stop",
            departureTime = departureTime,
            dayOfWeek = 1,
            variant = variant,
            departurePoint = "Center"
        )
    }
}
