package ru.slavgorod.transport.ui.components.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ScheduleTimeUtilsTest {

    @Test
    fun `resolveDepartureMillis returns same-day departure`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        val departure = resolveDepartureMillis("10:45", currentTimeMillis)

        assertEquals(timeMillis(hour = 10, minute = 45), departure)
    }

    @Test
    fun `resolveDepartureMillis moves departed time to tomorrow`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        val departure = resolveDepartureMillis("09:55", currentTimeMillis)

        assertEquals(currentTimeMillis + (23 * 60 + 55) * 60_000L, departure)
    }

    @Test
    fun `resolveDepartureMillis returns null for invalid time`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        assertNull(resolveDepartureMillis("not-time", currentTimeMillis))
    }

    @Test
    fun `resolveElapsedDepartureMillis returns time since today's departure`() {
        val elapsed = resolveElapsedDepartureMillis("09:45", timeMillis(hour = 10, minute = 0))

        assertEquals(15 * 60_000L, elapsed)
    }

    @Test
    fun `buildScheduleTimerLabel includes seconds`() {
        assertEquals("13м 12с", buildScheduleTimerLabel(13 * 60_000L + 12_000L))
        assertEquals("1ч 2м 03с", buildScheduleTimerLabel(3_723_000L))
    }

    private fun timeMillis(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 15)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
