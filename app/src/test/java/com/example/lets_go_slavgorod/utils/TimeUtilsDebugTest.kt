package com.example.lets_go_slavgorod.utils

import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Тесты для отладки проблем с временем
 */
class TimeUtilsDebugTest {
    
    @Test
    fun `test 23-34 time calculation`() {
        val currentTime = Calendar.getInstance()
        // Устанавливаем время на 22:00 для тестирования
        currentTime.set(Calendar.HOUR_OF_DAY, 22)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        val departureTime = "23:34"
        val minutes = TimeUtils.getTimeUntilDeparture(departureTime, currentTime)
        
        println("Current time: ${currentTime.get(Calendar.HOUR_OF_DAY)}:${currentTime.get(Calendar.MINUTE)}")
        println("Departure time: $departureTime")
        println("Time until departure: $minutes minutes")
        
        assertNotNull("Should return minutes for 23:34", minutes)
        assertTrue("Should be positive minutes", minutes!! > 0)
        assertEquals("Should be 94 minutes", 94, minutes)
    }
    
    @Test
    fun `test current time with 23-34`() {
        val currentTime = Calendar.getInstance()
        val departureTime = "23:34"
        val minutes = TimeUtils.getTimeUntilDeparture(departureTime, currentTime)
        
        println("Current time: ${currentTime.get(Calendar.HOUR_OF_DAY)}:${currentTime.get(Calendar.MINUTE)}")
        println("Departure time: $departureTime")
        println("Time until departure: $minutes minutes")
        
        // Время должно быть либо положительным (если еще не 23:34), либо null (если уже прошло)
        if (minutes != null) {
            assertTrue("Should be positive minutes", minutes > 0)
        }
    }
    
    @Test
    fun `test parseTime function`() {
        val timeString = "23:34"
        val calendar = TimeUtils.parseTime(timeString)
        
        assertEquals("Hour should be 23", 23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals("Minute should be 34", 34, calendar.get(Calendar.MINUTE))
        assertEquals("Second should be 0", 0, calendar.get(Calendar.SECOND))
        assertEquals("Millisecond should be 0", 0, calendar.get(Calendar.MILLISECOND))
    }
}
