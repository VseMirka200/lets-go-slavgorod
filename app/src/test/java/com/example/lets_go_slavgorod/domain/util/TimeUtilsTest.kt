package com.example.lets_go_slavgorod.domain.util

import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit тесты для TimeUtils
 * 
 * Проверяет корректность работы утилит для работы с временем:
 * - Форматирование времени
 * - Вычисление интервалов
 * - Работа с днями недели
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
class TimeUtilsTest {
    
    @Test
    fun `formatTime should format time correctly`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        
        // When
        val formattedTime = TimeUtils.formatTime(calendar)
        
        // Then
        assertEquals("14:30", formattedTime)
    }
    
    @Test
    fun `formatTime should handle single digit hours and minutes`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 5)
        }
        
        // When
        val formattedTime = TimeUtils.formatTime(calendar)
        
        // Then
        assertEquals("09:05", formattedTime)
    }
    
    @Test
    fun `getDayOfWeek should return correct day`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        
        // When
        val dayOfWeek = TimeUtils.getDayOfWeek(calendar)
        
        // Then
        assertEquals(1, dayOfWeek) // Monday = 1
    }
    
    @Test
    fun `isWeekend should return true for Saturday`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
        }
        
        // When
        val isWeekend = TimeUtils.isWeekend(calendar)
        
        // Then
        assertTrue(isWeekend)
    }
    
    @Test
    fun `isWeekend should return true for Sunday`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }
        
        // When
        val isWeekend = TimeUtils.isWeekend(calendar)
        
        // Then
        assertTrue(isWeekend)
    }
    
    @Test
    fun `isWeekend should return false for Monday`() {
        // Given
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }
        
        // When
        val isWeekend = TimeUtils.isWeekend(calendar)
        
        // Then
        assertFalse(isWeekend)
    }
    
    @Test
    fun `getMinutesUntil should calculate correctly`() {
        // Given
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        
        // When
        val minutesUntil = TimeUtils.getMinutesUntil(currentTime, targetTime)
        
        // Then
        assertEquals(30, minutesUntil)
    }
    
    @Test
    fun `getMinutesUntil should handle negative values`() {
        // Given
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 0)
        }
        
        // When
        val minutesUntil = TimeUtils.getMinutesUntil(currentTime, targetTime)
        
        // Then
        assertEquals(-30, minutesUntil)
    }
}
