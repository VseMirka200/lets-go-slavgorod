package ru.slavgorod.transport.ui.components.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ScheduleTimeUtilsTest {

    @Test
    fun `resolveDepartureDiffMinutes returns same-day departure`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        val diffMinutes = resolveDepartureDiffMinutes("10:45", currentTimeMillis)

        assertEquals(45, diffMinutes)
    }

    @Test
    fun `resolveDepartureDiffMinutes moves departed time to tomorrow`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        val diffMinutes = resolveDepartureDiffMinutes("09:55", currentTimeMillis)

        assertEquals(23 * 60 + 55, diffMinutes)
    }

    @Test
    fun `resolveDepartureDiffMinutes returns null for invalid time`() {
        val currentTimeMillis = timeMillis(hour = 10, minute = 0)

        assertNull(resolveDepartureDiffMinutes("not-time", currentTimeMillis))
    }

    @Test
    fun `buildCountdownLabelArgs resolves minutes`() {
        assertEquals(CountdownLabelArgs.Minutes(15), buildCountdownLabelArgs(15))
    }

    @Test
    fun `buildCountdownLabelArgs resolves full hours`() {
        assertEquals(CountdownLabelArgs.Hours(2), buildCountdownLabelArgs(120))
    }

    @Test
    fun `buildCountdownLabelArgs resolves hours and minutes`() {
        assertEquals(CountdownLabelArgs.HoursMinutes(2, "05"), buildCountdownLabelArgs(125))
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
