package com.example.lets_go_slavgorod.domain.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit тесты для ValidationUtils
 * 
 * Проверяет корректность валидации данных:
 * - Валидация ID маршрутов
 * - Валидация названий
 * - Валидация времени
 * - Валидация URL
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
class ValidationUtilsTest {
    
    @Test
    fun `isValidRouteId should return true for valid IDs`() {
        // Given
        val validIds = listOf("102", "102B", "1", "3", "4")
        
        // When & Then
        validIds.forEach { id ->
            assertTrue("ID '$id' should be valid", ValidationUtils.isValidRouteId(id))
        }
    }
    
    @Test
    fun `isValidRouteId should return false for invalid IDs`() {
        // Given
        val invalidIds = listOf("", " ", "abc", "102C", "0", "-1")
        
        // When & Then
        invalidIds.forEach { id ->
            assertFalse("ID '$id' should be invalid", ValidationUtils.isValidRouteId(id))
        }
    }
    
    @Test
    fun `isValidRouteNumber should return true for valid numbers`() {
        // Given
        val validNumbers = listOf("102", "102Б", "1", "3", "4")
        
        // When & Then
        validNumbers.forEach { number ->
            assertTrue("Number '$number' should be valid", ValidationUtils.isValidRouteNumber(number))
        }
    }
    
    @Test
    fun `isValidRouteNumber should return false for invalid numbers`() {
        // Given
        val invalidNumbers = listOf("", " ", "abc", "0", "-1")
        
        // When & Then
        invalidNumbers.forEach { number ->
            assertFalse("Number '$number' should be invalid", ValidationUtils.isValidRouteNumber(number))
        }
    }
    
    @Test
    fun `isValidRouteName should return true for valid names`() {
        // Given
        val validNames = listOf("Автобус №102", "Маршрут 1", "Кольцевой")
        
        // When & Then
        validNames.forEach { name ->
            assertTrue("Name '$name' should be valid", ValidationUtils.isValidRouteName(name))
        }
    }
    
    @Test
    fun `isValidRouteName should return false for invalid names`() {
        // Given
        val invalidNames = listOf("", " ", "a", "x".repeat(100))
        
        // When & Then
        invalidNames.forEach { name ->
            assertFalse("Name '$name' should be invalid", ValidationUtils.isValidRouteName(name))
        }
    }
    
    @Test
    fun `isValidTime should return true for valid times`() {
        // Given
        val validTimes = listOf("06:25", "14:30", "23:59", "00:00")
        
        // When & Then
        validTimes.forEach { time ->
            assertTrue("Time '$time' should be valid", ValidationUtils.isValidTime(time))
        }
    }
    
    @Test
    fun `isValidTime should return false for invalid times`() {
        // Given
        val invalidTimes = listOf("", " ", "25:00", "12:60", "abc", "12:5")
        
        // When & Then
        invalidTimes.forEach { time ->
            assertFalse("Time '$time' should be invalid", ValidationUtils.isValidTime(time))
        }
    }
    
    @Test
    fun `isValidUrl should return true for valid URLs`() {
        // Given
        val validUrls = listOf(
            "https://api.github.com",
            "https://raw.githubusercontent.com/user/repo/main/file.json",
            "https://example.com"
        )
        
        // When & Then
        validUrls.forEach { url ->
            assertTrue("URL '$url' should be valid", ValidationUtils.isValidUrl(url))
        }
    }
    
    @Test
    fun `isValidUrl should return false for invalid URLs`() {
        // Given
        val invalidUrls = listOf("", " ", "http://example.com", "ftp://example.com", "not-a-url")
        
        // When & Then
        invalidUrls.forEach { url ->
            assertFalse("URL '$url' should be invalid", ValidationUtils.isValidUrl(url))
        }
    }
    
    @Test
    fun `sanitizeString should clean input correctly`() {
        // Given
        val input = "  Test String  \n\t"
        val expected = "Test String"
        
        // When
        val result = ValidationUtils.sanitizeString(input)
        
        // Then
        assertEquals(expected, result)
    }
    
    @Test
    fun `sanitizeString should handle null input`() {
        // Given
        val input: String? = null
        
        // When
        val result = ValidationUtils.sanitizeString(input)
        
        // Then
        assertEquals("", result)
    }
    
    @Test
    fun `isValidColor should return true for valid colors`() {
        // Given
        val validColors = listOf("#FF6200EE", "#FF1976D2", "#FF4CAF50")
        
        // When & Then
        validColors.forEach { color ->
            assertTrue("Color '$color' should be valid", ValidationUtils.isValidColor(color))
        }
    }
    
    @Test
    fun `isValidColor should return false for invalid colors`() {
        // Given
        val invalidColors = listOf("", " ", "red", "#FF", "#FF6200EEFF", "not-a-color")
        
        // When & Then
        invalidColors.forEach { color ->
            assertFalse("Color '$color' should be invalid", ValidationUtils.isValidColor(color))
        }
    }
}
