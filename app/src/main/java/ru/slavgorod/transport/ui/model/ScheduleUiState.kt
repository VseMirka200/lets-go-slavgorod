package ru.slavgorod.transport.ui.model

import androidx.compose.runtime.Immutable
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.data.model.BusSchedule

@Immutable
data class ScheduleUiState(
    val route: BusRoute,
    val departurePoints: List<DeparturePointSchedules>
)

@Immutable
data class DeparturePointSchedules(
    val name: String,
    val schedules: List<BusSchedule>,
    val nextUpcomingId: String? = null
)
