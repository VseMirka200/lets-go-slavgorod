package com.example.lets_go_slavgorod.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты для ScheduleUtils
 */
class ScheduleUtilsTest {
    
    @Test
    fun `generateSchedules for route 102 returns correct schedules`() {
        val schedules = ScheduleUtils.generateSchedules("102")
        
        assertNotNull("Schedules should not be null", schedules)
        assertTrue("Should have schedules for route 102", schedules.isNotEmpty())
        
        // Проверяем, что все расписания для маршрута 102
        schedules.forEach { schedule ->
            assertEquals("All schedules should be for route 102", "102", schedule.routeId)
        }
        
        // Проверяем, что есть расписания из Славгорода
        val slavgorodSchedules = schedules.filter { it.departurePoint == "Рынок (Славгород)" }
        assertTrue("Should have schedules from Slavgorod", slavgorodSchedules.isNotEmpty())
        
        // Проверяем, что есть расписания из Ярового
        val yarovoeSchedules = schedules.filter { it.departurePoint == "МСЧ-128 (Яровое)" }
        assertTrue("Should have schedules from Yarovoe", yarovoeSchedules.isNotEmpty())
    }
    
    @Test
    fun `generateSchedules for route 1 returns correct schedules`() {
        val schedules = ScheduleUtils.generateSchedules("1")
        
        assertNotNull("Schedules should not be null", schedules)
        assertTrue("Should have schedules for route 1", schedules.isNotEmpty())
        
        // Проверяем, что все расписания для маршрута 1
        schedules.forEach { schedule ->
            assertEquals("All schedules should be for route 1", "1", schedule.routeId)
        }
        
        // Проверяем, что есть расписания из Вокзала
        val vokzalSchedules = schedules.filter { it.departurePoint == "вокзал" }
        assertTrue("Should have schedules from Vokzal", vokzalSchedules.isNotEmpty())
        
        // Проверяем, что есть расписания из Совхоза
        val sovhozSchedules = schedules.filter { it.departurePoint == "совхоз" }
        assertTrue("Should have schedules from Sovhoz", sovhozSchedules.isNotEmpty())
    }
    
    @Test
    fun `generateSchedules for unknown route returns empty list`() {
        val schedules = ScheduleUtils.generateSchedules("999")
        
        assertNotNull("Schedules should not be null", schedules)
        assertTrue("Should return empty list for unknown route", schedules.isEmpty())
    }
    
    @Test
    fun `all schedules have valid IDs`() {
        val route102Schedules = ScheduleUtils.generateSchedules("102")
        val route1Schedules = ScheduleUtils.generateSchedules("1")
        
        val allSchedules = route102Schedules + route1Schedules
        
        allSchedules.forEach { schedule ->
            assertTrue("Schedule ID should not be empty", schedule.id.isNotEmpty())
            assertTrue("Schedule ID should be unique", 
                allSchedules.count { it.id == schedule.id } == 1)
        }
    }
    
    @Test
    fun `all schedules have valid times`() {
        val route102Schedules = ScheduleUtils.generateSchedules("102")
        val route1Schedules = ScheduleUtils.generateSchedules("1")
        
        val allSchedules = route102Schedules + route1Schedules
        
        allSchedules.forEach { schedule ->
            assertTrue("Departure time should not be empty", schedule.departureTime.isNotEmpty())
            assertTrue("Departure time should be in HH:mm format", 
                schedule.departureTime.matches(Regex("\\d{2}:\\d{2}")))
        }
    }
}
