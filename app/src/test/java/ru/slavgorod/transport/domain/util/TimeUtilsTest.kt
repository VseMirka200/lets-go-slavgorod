package ru.slavgorod.transport.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TimeUtilsTest {

    @Test
    fun `formatTime should format time correctly`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }

        val formattedTime = TimeUtils.formatTime(calendar)

        assertEquals("14:30", formattedTime)
    }

    @Test
    fun `formatTime should handle single digit hours and minutes`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }

        val formattedTime = TimeUtils.formatTime(calendar)

        assertEquals("09:05", formattedTime)
    }

    @Test
    fun `getDayOfWeek should return correct day`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val dayOfWeek = TimeUtils.getDayOfWeek(calendar)

        assertEquals(1, dayOfWeek) // Monday = 1
    }

    @Test
    fun `isWeekend should return true for Saturday`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }

        val isWeekend = TimeUtils.isWeekend(calendar)

        assertTrue(isWeekend)
    }

    @Test
    fun `isWeekend should return true for Sunday`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val isWeekend = TimeUtils.isWeekend(calendar)

        assertTrue(isWeekend)
    }

    @Test
    fun `isWeekend should return false for Monday`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val isWeekend = TimeUtils.isWeekend(calendar)

        assertFalse(isWeekend)
    }

    @Test
    fun `getMinutesUntil should calculate correctly`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }

        val minutesUntil = TimeUtils.getMinutesUntil(currentTime, targetTime)

        assertEquals(30, minutesUntil)
    }

    @Test
    fun `getMinutesUntil should handle negative values`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }

        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }

        val minutesUntil = TimeUtils.getMinutesUntil(currentTime, targetTime)

        assertEquals(-30, minutesUntil)
    }
}
