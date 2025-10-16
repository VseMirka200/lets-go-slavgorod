package com.example.lets_go_slavgorod.utils

import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Тесты для проверки обратного отсчета времени
 */
class TimeUtilsCountdownTest {
    
    @Test
    fun `getTimeUntilDeparture should return correct minutes for future time`() {
        val currentTime = Calendar.getInstance()
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        // Тестируем время через 30 минут
        val futureTime = "10:30"
        val minutes = TimeUtils.getTimeUntilDeparture(futureTime, currentTime)
        
        assertNotNull("Should return minutes for future time", minutes)
        assertEquals("Should return 30 minutes", 30, minutes)
    }
    
    @Test
    fun `getTimeUntilDeparture should return null for past time`() {
        val currentTime = Calendar.getInstance()
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        // Тестируем время 30 минут назад
        val pastTime = "09:30"
        val minutes = TimeUtils.getTimeUntilDeparture(pastTime, currentTime)
        
        assertNull("Should return null for past time", minutes)
    }
    
    @Test
    fun `getTimeUntilDepartureWithSeconds should return correct minutes and seconds`() {
        val currentTime = Calendar.getInstance()
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        // Тестируем время через 5 минут 30 секунд
        val futureTime = "10:05"
        val timeWithSeconds = TimeUtils.getTimeUntilDepartureWithSeconds(futureTime, currentTime)
        
        assertNotNull("Should return time with seconds for future time", timeWithSeconds)
        assertEquals("Should return 5 minutes", 5, timeWithSeconds?.first)
        assertEquals("Should return 0 seconds", 0, timeWithSeconds?.second)
    }
    
    @Test
    fun `formatTimeUntilDepartureWithSeconds should format correctly`() {
        val formatted = TimeUtils.formatTimeUntilDepartureWithSeconds(5, 30, "10:05")
        
        assertNotNull("Should return formatted string", formatted)
        assertTrue("Should contain minutes", formatted.contains("5"))
        assertTrue("Should contain seconds", formatted.contains("30"))
    }
    
    @Test
    fun `formatTimeUntilDepartureWithExactTime should format correctly`() {
        val formatted = TimeUtils.formatTimeUntilDepartureWithExactTime(30, "10:30")
        
        assertNotNull("Should return formatted string", formatted)
        assertTrue("Should contain minutes", formatted.contains("30"))
        assertTrue("Should contain departure time", formatted.contains("10:30"))
    }
    
    @Test
    fun `isNextDeparture should return true for closest schedule`() {
        val currentTime = Calendar.getInstance()
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        val schedules = listOf(
            com.example.lets_go_slavgorod.data.model.BusSchedule("1", "1", "Test", "10:30", 1, notes = null, departurePoint = "Test Point"),
            com.example.lets_go_slavgorod.data.model.BusSchedule("2", "1", "Test", "11:00", 1, notes = null, departurePoint = "Test Point")
        )
        
        val isNext = TimeUtils.isNextDeparture(schedules[0], schedules, currentTime)
        
        assertTrue("Should be next departure", isNext)
    }
    
    @Test
    fun `getNextDeparture should return closest schedule`() {
        val currentTime = Calendar.getInstance()
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        val schedules = listOf(
            com.example.lets_go_slavgorod.data.model.BusSchedule("1", "1", "Test", "10:30", 1, notes = null, departurePoint = "Test Point"),
            com.example.lets_go_slavgorod.data.model.BusSchedule("2", "1", "Test", "11:00", 1, notes = null, departurePoint = "Test Point")
        )
        
        val nextDeparture = TimeUtils.getNextDeparture(schedules, currentTime)
        
        assertNotNull("Should return next departure", nextDeparture)
        assertEquals("Should return closest schedule", "1", nextDeparture?.id)
    }
}
