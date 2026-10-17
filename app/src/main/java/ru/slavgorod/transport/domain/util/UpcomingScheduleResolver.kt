package ru.slavgorod.transport.domain.util

import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.data.model.BusSchedule
import timber.log.Timber
import java.time.LocalTime
import java.time.LocalDate
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
        return parseTime(departureTime)?.let { departureTime ->
            nextDepartureMillis(departureTime, currentTime)?.let { departureTimeMillis ->
                DepartureCandidate(this, departureTimeMillis)
            }
        }
    }

    private fun parseTime(departureTime: String): LocalTime? {
        return try {
            LocalTime.parse(departureTime, timeFormatter)
        } catch (error: DateTimeParseException) {
            Timber.w(error, AppText.get(R.string.schedule_invalid_time), departureTime)
            null
        }
    }

    private fun BusSchedule.nextDepartureMillis(
        departureTime: LocalTime,
        currentTime: Calendar
    ): Long? {
        val startDate = LocalDate.of(
            currentTime.get(Calendar.YEAR),
            currentTime.get(Calendar.MONTH) + 1,
            currentTime.get(Calendar.DAY_OF_MONTH)
        )
        repeat(MAX_SERVICE_LOOK_AHEAD_DAYS) { offset ->
            val date = startDate.plusDays(offset.toLong())
            if (!serviceCalendar.runsOn(date)) return@repeat
            val departure = buildDepartureCalendar(currentTime, date, departureTime)
            if (isVisibleDeparture(departure.timeInMillis, currentTime.timeInMillis)) {
                return departure.timeInMillis
            }
        }
        return null
    }

    private fun List<DepartureCandidate>.firstVisibleScheduleId(currentTimeMillis: Long): String? {
        return firstOrNull { candidate ->
            isVisibleDeparture(candidate.departureTimeMillis, currentTimeMillis)
        }?.schedule?.id
    }

    private fun buildDepartureCalendar(
        currentTime: Calendar,
        date: LocalDate,
        localTime: LocalTime
    ): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = currentTime.timeInMillis
            set(Calendar.YEAR, date.year)
            set(Calendar.MONTH, date.monthValue - 1)
            set(Calendar.DAY_OF_MONTH, date.dayOfMonth)
            set(Calendar.HOUR_OF_DAY, localTime.hour)
            set(Calendar.MINUTE, localTime.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private data class DepartureCandidate(
        val schedule: BusSchedule,
        val departureTimeMillis: Long
    )

    private fun isVisibleDeparture(departureTimeMillis: Long, currentTimeMillis: Long): Boolean {
        return departureTimeMillis - currentTimeMillis > MINIMUM_VISIBLE_COUNTDOWN_MILLIS
    }

    private const val MAX_SERVICE_LOOK_AHEAD_DAYS = 732
}
