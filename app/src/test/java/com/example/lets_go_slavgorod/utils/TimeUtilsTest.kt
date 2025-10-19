package com.example.lets_go_slavgorod.utils

import com.example.lets_go_slavgorod.data.model.BusSchedule
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import timber.log.Timber
import java.util.*

/**
 * Unit тесты для TimeUtils
 * 
 * Тестируют функции работы со временем и расписаниями.
 * 
 * @author VseMirka200
 * @version 1.0
 */
class TimeUtilsTest {

    @Before
    fun setup() {
        // Инициализация Timber для тестов (без вывода)
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Не выводим логи в тестах
            }
        })
    }

    // ===== Тесты для getTimeUntilDeparture =====
    
    @Test
    fun `getTimeUntilDeparture calculates correct time difference`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Отправление в 8:30 (через 30 минут)
        val result = TimeUtils.getTimeUntilDeparture("08:30", currentTime)
        assertEquals(30, result)
    }

    @Test
    fun `getTimeUntilDeparture returns null for past time`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        
        // Отправление было в 8:00
        val result = TimeUtils.getTimeUntilDeparture("08:00", currentTime)
        assertNull(result)
    }

    @Test
    fun `getTimeUntilDeparture handles midnight correctly`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
        }
        
        // Отправление в 00:05 (через 10 минут)
        val result = TimeUtils.getTimeUntilDeparture("00:05", currentTime)
        // Должно вернуть null, так как это считается прошедшим временем в рамках одного дня
        assertNull(result)
    }

    // ===== Тесты для getCurrentDayOfWeek =====
    
    @Test
    fun `getCurrentDayOfWeek returns valid day number`() {
        val dayOfWeek = TimeUtils.getCurrentDayOfWeek()
        assertTrue(dayOfWeek in 1..7)
    }

    // ===== Тесты для filterSchedulesByTime =====
    
    @Test
    fun `filterSchedulesByTime filters past schedules`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
        }
        
        val schedules = listOf(
            createSchedule("08:00"),  // Прошло
            createSchedule("09:30"),  // Прошло
            createSchedule("10:30"),  // Будущее
            createSchedule("11:00")   // Будущее
        )
        
        val filtered = TimeUtils.filterSchedulesByTime(schedules, currentTime)
        assertEquals(2, filtered.size)
        assertEquals("10:30", filtered[0].departureTime)
        assertEquals("11:00", filtered[1].departureTime)
    }

    @Test
    fun `filterSchedulesByTime returns empty list when all schedules are past`() {
        val currentTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
        }
        
        val schedules = listOf(
            createSchedule("08:00"),
            createSchedule("10:00"),
            createSchedule("12:00")
        )
        
        val filtered = TimeUtils.filterSchedulesByTime(schedules, currentTime)
        assertTrue(filtered.isEmpty())
    }

    // ===== Тесты для groupSchedulesByDayOfWeek =====
    
    @Test
    fun `groupSchedulesByDayOfWeek groups schedules correctly`() {
        val schedules = listOf(
            createSchedule("08:00", dayOfWeek = 1), // Воскресенье
            createSchedule("09:00", dayOfWeek = 1), // Воскресенье
            createSchedule("08:00", dayOfWeek = 2), // Понедельник
            createSchedule("10:00", dayOfWeek = 3)  // Вторник
        )
        
        val grouped = TimeUtils.groupSchedulesByDayOfWeek(schedules)
        
        assertEquals(3, grouped.size)
        assertEquals(2, grouped[1]?.size) // 2 расписания на воскресенье
        assertEquals(1, grouped[2]?.size) // 1 расписание на понедельник
        assertEquals(1, grouped[3]?.size) // 1 расписание на вторник
    }

    @Test
    fun `groupSchedulesByDayOfWeek returns empty map for empty list`() {
        val grouped = TimeUtils.groupSchedulesByDayOfWeek(emptyList())
        assertTrue(grouped.isEmpty())
    }

    // ===== Вспомогательные функции =====
    
    private fun createSchedule(
        departureTime: String,
        dayOfWeek: Int = 1,
        routeId: String = "102"
    ): BusSchedule {
        return BusSchedule(
            id = "test_$departureTime",
            routeId = routeId,
            stopName = "Тестовая остановка",
            departureTime = departureTime,
            dayOfWeek = dayOfWeek,
            departurePoint = "Тестовый пункт"
        )
    }
}
