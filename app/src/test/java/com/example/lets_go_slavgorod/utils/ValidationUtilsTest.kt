package com.example.lets_go_slavgorod.utils

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import timber.log.Timber

/**
 * Unit тесты для ValidationUtils
 * 
 * Тестируют все методы валидации данных в приложении.
 * 
 * @author VseMirka200
 * @version 1.0
 */
class ValidationUtilsTest {

    @Before
    fun setup() {
        // Инициализация Timber для тестов (без вывода)
        Timber.plant(object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                // Не выводим логи в тестах
            }
        })
    }

    // ===== Тесты для isValidTime =====
    
    @Test
    fun `isValidTime returns true for valid time formats`() {
        assertTrue(ValidationUtils.isValidTime("00:00"))
        assertTrue(ValidationUtils.isValidTime("08:30"))
        assertTrue(ValidationUtils.isValidTime("12:00"))
        assertTrue(ValidationUtils.isValidTime("23:59"))
    }

    @Test
    fun `isValidTime returns false for invalid time formats`() {
        assertFalse(ValidationUtils.isValidTime(""))
        assertFalse(ValidationUtils.isValidTime(" "))
        assertFalse(ValidationUtils.isValidTime("25:00"))
        assertFalse(ValidationUtils.isValidTime("08:60"))
        assertFalse(ValidationUtils.isValidTime("8:30"))  // Должно быть 08:30
        assertFalse(ValidationUtils.isValidTime("08-30")) // Неправильный разделитель
        assertFalse(ValidationUtils.isValidTime("abc:de"))
    }

    // ===== Тесты для isValidDayOfWeek =====
    
    @Test
    fun `isValidDayOfWeek returns true for valid days`() {
        assertTrue(ValidationUtils.isValidDayOfWeek(1)) // Воскресенье
        assertTrue(ValidationUtils.isValidDayOfWeek(2)) // Понедельник
        assertTrue(ValidationUtils.isValidDayOfWeek(7)) // Суббота
    }

    @Test
    fun `isValidDayOfWeek returns false for invalid days`() {
        assertFalse(ValidationUtils.isValidDayOfWeek(0))
        assertFalse(ValidationUtils.isValidDayOfWeek(8))
        assertFalse(ValidationUtils.isValidDayOfWeek(-1))
        assertFalse(ValidationUtils.isValidDayOfWeek(100))
    }

    // ===== Тесты для isValidRouteId =====
    
    @Test
    fun `isValidRouteId returns true for valid route IDs`() {
        assertTrue(ValidationUtils.isValidRouteId("1"))
        assertTrue(ValidationUtils.isValidRouteId("102"))
        assertTrue(ValidationUtils.isValidRouteId("route_001"))
    }

    @Test
    fun `isValidRouteId returns false for invalid route IDs`() {
        assertFalse(ValidationUtils.isValidRouteId(""))
        assertFalse(ValidationUtils.isValidRouteId(" "))
        assertFalse(ValidationUtils.isValidRouteId("   "))
    }

    // ===== Тесты для isValidRouteNumber =====
    
    @Test
    fun `isValidRouteNumber returns true for valid route numbers`() {
        assertTrue(ValidationUtils.isValidRouteNumber("1"))
        assertTrue(ValidationUtils.isValidRouteNumber("102"))
        assertTrue(ValidationUtils.isValidRouteNumber("2А"))
        assertTrue(ValidationUtils.isValidRouteNumber("15Б"))
    }

    @Test
    fun `isValidRouteNumber returns false for invalid route numbers`() {
        assertFalse(ValidationUtils.isValidRouteNumber(""))
        assertFalse(ValidationUtils.isValidRouteNumber(" "))
        assertFalse(ValidationUtils.isValidRouteNumber("   "))
    }

    // ===== Тесты для isValidRouteName =====
    
    @Test
    fun `isValidRouteName returns true for valid route names`() {
        assertTrue(ValidationUtils.isValidRouteName("Автобус №1"))
        assertTrue(ValidationUtils.isValidRouteName("Маршрут вокзал — совхоз"))
        assertTrue(ValidationUtils.isValidRouteName("Route 102"))
    }

    @Test
    fun `isValidRouteName returns false for invalid route names`() {
        assertFalse(ValidationUtils.isValidRouteName(""))
        assertFalse(ValidationUtils.isValidRouteName(" "))
        assertFalse(ValidationUtils.isValidRouteName("   "))
    }

    // ===== Тесты для isValidStopName =====
    
    @Test
    fun `isValidStopName returns true for valid stop names`() {
        assertTrue(ValidationUtils.isValidStopName("Рынок"))
        assertTrue(ValidationUtils.isValidStopName("Центральная площадь"))
        assertTrue(ValidationUtils.isValidStopName("МСЧ-128"))
    }

    @Test
    fun `isValidStopName returns false for invalid stop names`() {
        assertFalse(ValidationUtils.isValidStopName(""))
        assertFalse(ValidationUtils.isValidStopName(" "))
        assertFalse(ValidationUtils.isValidStopName("   "))
    }

    // ===== Тесты для isValidEmail =====
    
    @Test
    fun `isValidEmail returns true for valid emails`() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"))
        assertTrue(ValidationUtils.isValidEmail("test.email@domain.co.uk"))
        assertTrue(ValidationUtils.isValidEmail("name+tag@email.com"))
    }

    @Test
    fun `isValidEmail returns false for invalid emails`() {
        assertFalse(ValidationUtils.isValidEmail(""))
        assertFalse(ValidationUtils.isValidEmail(" "))
        assertFalse(ValidationUtils.isValidEmail("notanemail"))
        assertFalse(ValidationUtils.isValidEmail("@example.com"))
        assertFalse(ValidationUtils.isValidEmail("user@"))
        assertFalse(ValidationUtils.isValidEmail("user@.com"))
    }

    // ===== Тесты для isValidUrl =====
    
    @Test
    fun `isValidUrl returns true for valid URLs`() {
        assertTrue(ValidationUtils.isValidUrl("https://example.com"))
        assertTrue(ValidationUtils.isValidUrl("http://test.com/path"))
        assertTrue(ValidationUtils.isValidUrl("https://api.github.com/repos/user/repo"))
    }

    @Test
    fun `isValidUrl returns false for invalid URLs`() {
        assertFalse(ValidationUtils.isValidUrl(""))
        assertFalse(ValidationUtils.isValidUrl(" "))
        assertFalse(ValidationUtils.isValidUrl("not a url"))
        assertFalse(ValidationUtils.isValidUrl("ftp://example.com")) // Только http/https
    }

    // ===== Тесты для isValidColor =====
    
    @Test
    fun `isValidColor returns true for valid colors`() {
        assertTrue(ValidationUtils.isValidColor("#FF5722FF"))
        assertTrue(ValidationUtils.isValidColor("#001976D2"))
        assertTrue(ValidationUtils.isValidColor("#FFFFFFFF"))
        assertTrue(ValidationUtils.isValidColor("#00000000"))
    }

    @Test
    fun `isValidColor returns false for invalid colors`() {
        assertFalse(ValidationUtils.isValidColor(""))
        assertFalse(ValidationUtils.isValidColor(" "))
        assertFalse(ValidationUtils.isValidColor("#FF5722")) // Слишком короткий
        assertFalse(ValidationUtils.isValidColor("FF5722FF")) // Нет #
        assertFalse(ValidationUtils.isValidColor("#GG5722FF")) // Неверные символы
    }

    // ===== Тесты для sanitizeString =====
    
    @Test
    fun `sanitizeString removes leading and trailing whitespace`() {
        assertEquals("test", ValidationUtils.sanitizeString("  test  "))
        assertEquals("hello world", ValidationUtils.sanitizeString("hello world"))
        assertEquals("", ValidationUtils.sanitizeString("   "))
    }

    @Test
    fun `sanitizeString returns empty string for null`() {
        assertEquals("", ValidationUtils.sanitizeString(null))
    }
}

