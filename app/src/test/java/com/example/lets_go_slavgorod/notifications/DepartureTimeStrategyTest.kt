package com.example.lets_go_slavgorod.notifications

import com.example.lets_go_slavgorod.domain.notification.AllDaysStrategy
import com.example.lets_go_slavgorod.domain.notification.SelectedDaysStrategy
import com.example.lets_go_slavgorod.domain.notification.WeekdaysStrategy
import com.example.lets_go_slavgorod.domain.notification.DisabledStrategy
import org.junit.Test
import java.util.Calendar
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit тесты для Strategy Pattern в DepartureTimeStrategy
 * 
 * Проверяет правильность вычисления следующего времени отправления
 * для разных режимов уведомлений.
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
class DepartureTimeStrategyTest {
    
    @Test
    fun `AllDaysStrategy calculates next day if time passed today`() {
        // Arrange
        val strategy = AllDaysStrategy()
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10) // Время в прошлом
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0, "Should return valid timestamp")
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        assertEquals(10, resultCalendar.get(Calendar.HOUR_OF_DAY), "Hour should be 10")
        assertEquals(30, resultCalendar.get(Calendar.MINUTE), "Minute should be 30")
        assertTrue(resultCalendar.after(now), "Next time should be in future")
    }
    
    @Test
    fun `AllDaysStrategy returns same day if time is in future`() {
        // Arrange
        val strategy = AllDaysStrategy()
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15) // Время в будущем
            set(Calendar.MINUTE, 30)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0)
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должно быть сегодня
        assertEquals(
            now.get(Calendar.DAY_OF_YEAR),
            resultCalendar.get(Calendar.DAY_OF_YEAR)
        )
    }
    
    @Test
    fun `WeekdaysStrategy skips saturday to monday`() {
        // Arrange
        val strategy = WeekdaysStrategy()
        
        // Суббота, 15:00
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0)
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должен быть понедельник
        assertEquals(
            Calendar.MONDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should skip weekend to Monday"
        )
    }
    
    @Test
    fun `WeekdaysStrategy skips sunday to monday`() {
        // Arrange
        val strategy = WeekdaysStrategy()
        
        // Воскресенье
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 15)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        assertEquals(Calendar.MONDAY, resultCalendar.get(Calendar.DAY_OF_WEEK))
    }
    
    @Test
    fun `SelectedDaysStrategy finds next selected day when target day is selected`() {
        // Arrange
        val selectedDays = setOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.FRIDAY
        )
        val strategy = SelectedDaysStrategy(selectedDays, java.time.DayOfWeek.MONDAY)
        
        // Вторник (не выбранный день)
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0)
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должна быть среда (следующий выбранный день)
        assertEquals(
            Calendar.WEDNESDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should be Wednesday (next selected day from Tuesday)"
        )
    }
    
    @Test
    fun `SelectedDaysStrategy returns -1 when target day is not selected`() {
        // Arrange
        val selectedDays = setOf(
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.FRIDAY
        )
        val strategy = SelectedDaysStrategy(selectedDays, java.time.DayOfWeek.MONDAY)
        
        // Вторник (не выбранный день)
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertEquals(-1L, result, "Should return -1 when target day (Monday) is not in selected days")
    }
    
    @Test
    fun `SelectedDaysStrategy finds next selected day when target day is null`() {
        // Arrange
        val selectedDays = setOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.FRIDAY
        )
        val strategy = SelectedDaysStrategy(selectedDays, null) // null означает "каждый день"
        
        // Вторник (не выбранный день)
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0)
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должна быть среда (следующий выбранный день)
        assertEquals(
            Calendar.WEDNESDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should be Wednesday (next selected day from Tuesday)"
        )
    }
    
    @Test
    fun `SelectedDaysStrategy with empty set returns -1`() {
        // Arrange
        val strategy = SelectedDaysStrategy(emptySet())
        val now = Calendar.getInstance()
        val baseTime = Calendar.getInstance()
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertEquals(-1L, result, "Should return -1 when no days selected")
    }
    
    @Test
    fun `DisabledStrategy always returns -1`() {
        // Arrange
        val strategy = DisabledStrategy()
        val now = Calendar.getInstance()
        val baseTime = Calendar.getInstance()
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertEquals(-1L, result, "Disabled strategy should always return -1")
    }
    
    @Test
    fun `AllDaysStrategy works with notification modes`() {
        // Arrange
        val strategy = AllDaysStrategy()
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15) // Время в будущем
            set(Calendar.MINUTE, 30)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0, "AllDaysStrategy should work every day")
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должно быть сегодня
        assertEquals(
            now.get(Calendar.DAY_OF_YEAR),
            resultCalendar.get(Calendar.DAY_OF_YEAR)
        )
    }
    
    @Test
    fun `WeekdaysStrategy respects weekday mode`() {
        // Arrange
        val strategy = WeekdaysStrategy()
        
        // Суббота, 15:00
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 0)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 30)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0, "WeekdaysStrategy should find next weekday")
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должен быть понедельник
        assertEquals(
            Calendar.MONDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should skip weekend to Monday"
        )
    }
    
    @Test
    fun `SelectedDaysStrategy respects selected days mode`() {
        // Arrange
        val selectedDays = setOf(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.FRIDAY
        )
        val strategy = SelectedDaysStrategy(selectedDays, null) // null означает "каждый день"
        
        // Вторник (не выбранный день)
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            set(Calendar.HOUR_OF_DAY, 15)
        }
        
        val baseTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
        }
        
        // Act
        val result = strategy.calculateNextTime(baseTime, now)
        
        // Assert
        assertTrue(result > 0, "SelectedDaysStrategy should find next selected day")
        
        val resultCalendar = Calendar.getInstance().apply {
            timeInMillis = result
        }
        
        // Должна быть среда (следующий выбранный день)
        assertEquals(
            Calendar.WEDNESDAY,
            resultCalendar.get(Calendar.DAY_OF_WEEK),
            "Should be Wednesday (next selected day from Tuesday)"
        )
    }
}