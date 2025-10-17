package com.example.lets_go_slavgorod.utils

import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Интеграционные тесты для проверки работы обратного отсчета
 */
class CountdownIntegrationTest {
    
    @Test
    fun `ScheduleUtils should generate schedules with correct times`() {
        val schedules = ScheduleUtils.generateSchedules("102")
        
        assertNotNull("Schedules should not be null", schedules)
        assertTrue("Should have schedules", schedules.isNotEmpty())
        
        // Проверяем, что есть расписания с разными временами
        val times = schedules.map { it.departureTime }.distinct()
        assertTrue("Should have different times", times.size > 1)
        
        // Проверяем формат времени
        times.forEach { time ->
            assertTrue("Time should be in HH:mm format", time.matches(Regex("\\d{2}:\\d{2}")))
        }
    }
    
    @Test
    fun `TimeUtils should work with ScheduleUtils schedules`() {
        val schedules = ScheduleUtils.generateSchedules("102")
        val currentTime = Calendar.getInstance()
        
        // Устанавливаем время на 10:00
        currentTime.set(Calendar.HOUR_OF_DAY, 10)
        currentTime.set(Calendar.MINUTE, 0)
        currentTime.set(Calendar.SECOND, 0)
        currentTime.set(Calendar.MILLISECOND, 0)
        
        // Находим ближайший рейс
        val nextDeparture = TimeUtils.getNextDeparture(schedules, currentTime)
        
        assertNotNull("Should find next departure", nextDeparture)
        assertTrue("Should have valid departure time", nextDeparture?.departureTime?.isNotEmpty() == true)
        
        // Проверяем время до отправления
        val timeUntilDeparture = nextDeparture?.let { schedule ->
            TimeUtils.getTimeUntilDeparture(
                departureTime = schedule.departureTime,
                currentTime = currentTime
            )
        }
        
        assertNotNull("Should calculate time until departure", timeUntilDeparture)
        assertTrue("Time until departure should be positive", timeUntilDeparture!! > 0)
    }
    
    @Test
    fun `Countdown should work for different times`() {
        val currentTime = Calendar.getInstance()
        
        // Тестируем разные времена
        val testTimes = listOf("10:30", "11:00", "12:00", "15:30", "18:00")
        
        testTimes.forEach { time ->
            val minutes = TimeUtils.getTimeUntilDeparture(time, currentTime)
            if (minutes != null) {
                assertTrue("Time until departure should be positive for $time", minutes > 0)
                
                // Проверяем форматирование
                val formatted = TimeUtils.formatTimeUntilDeparture(minutes)
                assertNotNull("Should format time for $time", formatted)
                assertTrue("Formatted time should not be empty for $time", formatted.isNotEmpty())
            }
        }
    }
    
    @Test
    fun `NextDepartureHeader should work with real schedules`() {
        val schedules = ScheduleUtils.generateSchedules("102")
        val currentTime = Calendar.getInstance()
        
        // Находим ближайший рейс
        val nextDeparture = TimeUtils.getNextDeparture(schedules, currentTime)
        
        if (nextDeparture != null) {
            val timeUntilDeparture = TimeUtils.getTimeUntilDeparture(nextDeparture.departureTime, currentTime)
            
            if (timeUntilDeparture != null) {
                // Проверяем форматирование для ближайших рейсов
                val formatted = if (timeUntilDeparture < 10) {
                    val timeWithSeconds = TimeUtils.getTimeUntilDepartureWithSeconds(nextDeparture.departureTime, currentTime)
                    if (timeWithSeconds != null) {
                        TimeUtils.formatTimeUntilDepartureWithSeconds(
                            timeWithSeconds.first, 
                            timeWithSeconds.second
                        )
                    } else {
                        TimeUtils.formatTimeUntilDeparture(timeUntilDeparture)
                    }
                } else {
                    TimeUtils.formatTimeUntilDeparture(timeUntilDeparture)
                }
                
                assertNotNull("Should format time for next departure", formatted)
                assertTrue("Formatted time should not be empty", formatted.isNotEmpty())
            }
        }
    }
}