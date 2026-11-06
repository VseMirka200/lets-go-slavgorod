package ru.slavgorod.transport.ui.components.schedule

import ru.slavgorod.transport.data.model.BusSchedule

internal data class UpcomingScheduleEntry(
    val title: String,
    val inTransitSchedule: BusSchedule,
    val nextSchedule: BusSchedule
)
