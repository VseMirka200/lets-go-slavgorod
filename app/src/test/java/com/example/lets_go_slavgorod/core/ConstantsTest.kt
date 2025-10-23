package com.example.lets_go_slavgorod.core

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit тесты для Constants
 * 
 * Проверяет корректность констант приложения:
 * - Версии и названия
 * - Размеры и отступы
 * - Цвета и настройки
 * - Лимиты и таймауты
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
class ConstantsTest {
    
    @Test
    fun `APP_VERSION should be valid`() {
        // Given & When
        val version = Constants.APP_VERSION
        
        // Then
        assertTrue("Version should start with 'v'", version.startsWith("v"))
        assertTrue("Version should contain numbers", version.any { it.isDigit() })
    }
    
    @Test
    fun `APP_NAME should not be empty`() {
        // Given & When
        val name = Constants.APP_NAME
        
        // Then
        assertFalse("App name should not be empty", name.isBlank())
        assertTrue("App name should contain 'Славгород'", name.contains("Славгород"))
    }
    
    @Test
    fun `DATABASE_VERSION should be positive`() {
        // Given & When
        val version = Constants.DATABASE_VERSION
        
        // Then
        assertTrue("Database version should be positive", version > 0)
    }
    
    @Test
    fun `DATABASE_NAME should not be empty`() {
        // Given & When
        val name = Constants.DATABASE_NAME
        
        // Then
        assertFalse("Database name should not be empty", name.isBlank())
    }
    
    @Test
    fun `PADDING constants should be positive`() {
        // Given & When
        val small = Constants.PADDING_SMALL
        val medium = Constants.PADDING_MEDIUM
        val large = Constants.PADDING_LARGE
        
        // Then
        assertTrue("Small padding should be positive", small > 0)
        assertTrue("Medium padding should be positive", medium > 0)
        assertTrue("Large padding should be positive", large > 0)
        assertTrue("Medium should be larger than small", medium > small)
        assertTrue("Large should be larger than medium", large > medium)
    }
    
    @Test
    fun `COLOR constants should be valid hex colors`() {
        // Given & When
        val defaultColor = Constants.DEFAULT_ROUTE_COLOR
        val altColor = Constants.DEFAULT_ROUTE_COLOR_ALT
        val greenColor = Constants.DEFAULT_ROUTE_COLOR_GREEN
        
        // Then
        assertTrue("Default color should be valid hex", defaultColor.matches(Regex("#[0-9A-Fa-f]{8}")))
        assertTrue("Alt color should be valid hex", altColor.matches(Regex("#[0-9A-Fa-f]{8}")))
        assertTrue("Green color should be valid hex", greenColor.matches(Regex("#[0-9A-Fa-f]{8}")))
    }
    
    @Test
    fun `COLOR_ALPHA should be between 0 and 1`() {
        // Given & When
        val alpha = Constants.COLOR_ALPHA
        
        // Then
        assertTrue("Alpha should be between 0 and 1", alpha in 0f..1f)
    }
    
    @Test
    fun `NOTIFICATION_TIME_OPTIONS should be valid`() {
        // Given & When
        val options = Constants.NOTIFICATION_TIME_OPTIONS
        
        // Then
        assertTrue("Options should not be empty", options.isNotEmpty())
        assertTrue("All options should be positive", options.all { it > 0 })
        assertTrue("Options should be sorted", options == options.sorted())
    }
    
    @Test
    fun `DEFAULT_NOTIFICATION_LEAD_TIME should be in options`() {
        // Given & When
        val defaultTime = Constants.DEFAULT_NOTIFICATION_LEAD_TIME
        val options = Constants.NOTIFICATION_TIME_OPTIONS
        
        // Then
        assertTrue("Default time should be in options", options.contains(defaultTime))
    }
    
    @Test
    fun `TIMEOUT constants should be positive`() {
        // Given & When
        val connectionTimeout = Constants.REMOTE_CONNECTION_TIMEOUT
        val readTimeout = Constants.REMOTE_READ_TIMEOUT
        
        // Then
        assertTrue("Connection timeout should be positive", connectionTimeout > 0)
        assertTrue("Read timeout should be positive", readTimeout > 0)
    }
    
    @Test
    fun `CACHE constants should be positive`() {
        // Given & When
        val maxCacheSize = Constants.ROUTES_MAX_CACHE_SIZE
        val cleanupThreshold = Constants.ROUTES_CACHE_CLEANUP_THRESHOLD
        
        // Then
        assertTrue("Max cache size should be positive", maxCacheSize > 0)
        assertTrue("Cleanup threshold should be positive", cleanupThreshold > 0)
        assertTrue("Cleanup threshold should be less than max size", cleanupThreshold < maxCacheSize)
    }
    
    @Test
    fun `ROUTE_ID constants should be valid`() {
        // Given & When
        val routeIds = listOf(
            Constants.ROUTE_ID_102,
            Constants.ROUTE_ID_102B,
            Constants.ROUTE_ID_1,
            Constants.ROUTE_ID_3,
            Constants.ROUTE_ID_4
        )
        
        // Then
        routeIds.forEach { id ->
            assertFalse("Route ID should not be empty", id.isBlank())
        }
    }
    
    @Test
    fun `STOP constants should not be empty`() {
        // Given & When
        val stops = listOf(
            Constants.STOP_SLAVGOROD_RYNOK,
            Constants.STOP_YAROVOE_MCHS,
            Constants.STOP_YAROVOE_ZORI,
            Constants.STOP_ROUTE1_VOKZAL,
            Constants.STOP_ROUTE1_SOVHOZ
        )
        
        // Then
        stops.forEach { stop ->
            assertFalse("Stop name should not be empty", stop.isBlank())
        }
    }
}
