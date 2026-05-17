package ru.slavgorod.transport.domain.util

import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.data.model.BusSchedule
import timber.log.Timber
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar
import java.util.Locale

object UpcomingScheduleResolver {

    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)
    private const val MINIMUM_VISIBLE_COUNTDOWN_MILLIS = 60_000L

    fun findNextScheduleId(
        schedules: List<BusSchedule>,
        currentTime: Calendar = Calendar.getInstance()
    ): String? {
        val normalized = schedules
            .mapNotNull { schedule -> schedule.toDepartureCandidate(currentTime) }
            .sortedBy { candidate -> candidate.departureTimeMillis }

        if (normalized.isEmpty()) return null

        return normalized.firstVisibleScheduleId(currentTime.timeInMillis)
            ?: normalized.first().schedule.id
    }

    private fun BusSchedule.toDepartureCandidate(currentTime: Calendar): DepartureCandidate? {
        return parseTimeToMillis(departureTime, currentTime)?.let { departureTimeMillis ->
            DepartureCandidate(this, departureTimeMillis)
        }
    }

    private fun parseTimeToMillis(departureTime: String, currentTime: Calendar): Long? {
        return try {
            val localTime = LocalTime.parse(departureTime, timeFormatter)
            buildDepartureCalendar(currentTime, localTime).timeInMillis
        } catch (error: DateTimeParseException) {
            Timber.w(error, AppText.get(R.string.schedule_invalid_time), departureTime)
            null
        }
    }

    private fun List<DepartureCandidate>.firstVisibleScheduleId(currentTimeMillis: Long): String? {
        return firstOrNull { candidate ->
            isVisibleDeparture(candidate.departureTimeMillis, currentTimeMillis)
        }?.schedule?.id
    }

    private fun buildDepartureCalendar(currentTime: Calendar, localTime: LocalTime): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = currentTime.timeInMillis
            set(Calendar.HOUR_OF_DAY, localTime.hour)
            set(Calendar.MINUTE, localTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis < currentTime.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
    }

    private data class DepartureCandidate(
        val schedule: BusSchedule,
        val departureTimeMillis: Long
    )

    private fun isVisibleDeparture(departureTimeMillis: Long, currentTimeMillis: Long): Boolean {
        return departureTimeMillis - currentTimeMillis > MINIMUM_VISIBLE_COUNTDOWN_MILLIS
    }
}
