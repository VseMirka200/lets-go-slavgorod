package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ru.slavgorod.transport.R

@Composable
internal fun buildCountdownLabel(diffMinutes: Int): String {
    return when (val args = buildCountdownLabelArgs(diffMinutes)) {
        CountdownLabelArgs.AlreadyDeparted -> {
            stringResource(R.string.schedule_time_already_departed)
        }

        is CountdownLabelArgs.Minutes -> {
            stringResource(R.string.schedule_time_minutes, args.minutes)
        }

        is CountdownLabelArgs.Hours -> {
            stringResource(R.string.schedule_time_hours, args.hours)
        }

        is CountdownLabelArgs.HoursMinutes -> {
            stringResource(R.string.schedule_time_hours_minutes, args.hours, args.minutes)
        }
    }
}
