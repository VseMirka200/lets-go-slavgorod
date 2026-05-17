package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusSchedule

@Composable
fun extractDayLabel(schedule: BusSchedule): String? {
    return when (schedule.dayType?.lowercase()) {
        "weekday" -> stringResource(R.string.schedule_day_weekday)
        "weekend" -> stringResource(R.string.schedule_day_weekend)
        "daily" -> stringResource(R.string.schedule_day_daily)
        else -> extractLegacyDayLabel(schedule.notes ?: schedule.remark)
    }
}

@Composable
private fun extractLegacyDayLabel(notes: String?): String? {
    if (notes == null) return null
    val weekdayLabel = stringResource(R.string.schedule_day_weekday)
    val saturdayLabel = stringResource(R.string.schedule_day_saturday)
    val weekendLabel = stringResource(R.string.schedule_day_weekend)

    return when {
        notes.contains(weekdayLabel, ignoreCase = true) -> weekdayLabel
        notes.contains(
            saturdayLabel,
            ignoreCase = true
        ) -> saturdayLabel

        notes.contains(
            weekendLabel,
            ignoreCase = true
        ) -> weekendLabel

        else -> null
    }
}
