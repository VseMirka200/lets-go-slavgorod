package com.example.lets_go_slavgorod.repository

import android.content.Context
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit-тесты для BusRouteRepository
 * 
 * Проверяет корректность работы репозитория:
 * - Загрузка маршрутов
 * - Поиск по ID
 * - Поиск по запросу
 * - Валидация данных
 * - Кэширование
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
class BusRouteRepositoryTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var repository: BusRouteRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        repository = BusRouteRepository(null) // Без контекста для тестов
    }
    
    @Test
    fun `getAllRoutes returns non-empty list`() {
        // When
        val routes = repository.getAllRoutes()
        
        // Then
        assertTrue(routes.isNotEmpty(), "Routes should not be empty")
    }
    
    @Test
    fun `getAllRoutes returns valid routes`() {
        // When
        val routes = repository.getAllRoutes()
        
        // Then
        routes.forEach { route ->
            assertTrue(route.isValid(), "Route ${route.id} should be valid")
            assertTrue(route.id.isNotBlank(), "Route ID should not be blank")
            assertTrue(route.name.isNotBlank(), "Route name should not be blank")
        }
    }
    
    @Test
    fun `getRouteById returns correct route`() {
        // Given
        val routes = repository.getAllRoutes()
        val expectedRoute = routes.firstOrNull()
        
        // When
        val route = expectedRoute?.let { repository.getRouteById(it.id) }
        
        // Then
        assertNotNull(route, "Route should be found")
        assertEquals(expectedRoute?.id, route?.id)
        assertEquals(expectedRoute?.name, route?.name)
    }
    
    @Test
    fun `getRouteById returns null for invalid id`() {
        // When
        val route = repository.getRouteById("invalid_id_123456")
        
        // Then
        assertNull(route, "Route should not be found for invalid ID")
    }
    
    @Test
    fun `getRouteById returns null for null id`() {
        // When
        val route = repository.getRouteById(null)
        
        // Then
        assertNull(route, "Route should be null for null ID")
    }
    
    @Test
    fun `getRouteById returns null for blank id`() {
        // When
        val route = repository.getRouteById("")
        
        // Then
        assertNull(route, "Route should be null for blank ID")
    }
    
    @Test
    fun `searchRoutes filters by route number`() {
        // When
        val results = repository.searchRoutes("102")
        
        // Then
        assertTrue(results.isNotEmpty(), "Search should return results")
        assertTrue(
            results.all { it.routeNumber.contains("102", ignoreCase = true) },
            "All results should match search query"
        )
    }
    
    @Test
    fun `searchRoutes filters by route name`() {
        // When
        val results = repository.searchRoutes("Автобус")
        
        // Then
        assertTrue(results.isNotEmpty(), "Search should return results")
        assertTrue(
            results.all { it.name.contains("Автобус", ignoreCase = true) },
            "All results should contain 'Автобус'"
        )
    }
    
    @Test
    fun `searchRoutes is case insensitive`() {
        // When
        val lowerCase = repository.searchRoutes("автобус")
        val upperCase = repository.searchRoutes("АВТОБУС")
        val mixedCase = repository.searchRoutes("АвТоБуС")
        
        // Then
        assertEquals(lowerCase.size, upperCase.size, "Case should not affect results")
        assertEquals(lowerCase.size, mixedCase.size, "Case should not affect results")
    }
    
    @Test
    fun `searchRoutes returns all routes for empty query`() {
        // When
        val results = repository.searchRoutes("")
        val allRoutes = repository.getAllRoutes()
        
        // Then
        assertEquals(allRoutes.size, results.size, "Empty query should return all routes")
    }
    
    @Test
    fun `searchRoutes returns empty list for no matches`() {
        // When
        val results = repository.searchRoutes("несуществующий_маршрут_xyz")
        
        // Then
        assertTrue(results.isEmpty(), "Search should return empty list for no matches")
    }
    
    @Test
    fun `repository caches routes`() {
        // Given
        val firstCall = repository.getAllRoutes()
        
        // When
        val secondCall = repository.getAllRoutes()
        
        // Then
        assertEquals(firstCall.size, secondCall.size, "Cache should return same routes")
        assertEquals(firstCall, secondCall, "Cached routes should be identical")
    }
    
    @Test
    fun `all routes have unique IDs`() {
        // When
        val routes = repository.getAllRoutes()
        val ids = routes.map { it.id }
        val uniqueIds = ids.toSet()
        
        // Then
        assertEquals(
            ids.size,
            uniqueIds.size,
            "All route IDs should be unique"
        )
    }
    
    @Test
    fun `all routes have required fields`() {
        // When
        val routes = repository.getAllRoutes()
        
        // Then
        routes.forEach { route ->
            assertTrue(route.id.isNotBlank(), "Route ID should not be blank")
            assertTrue(route.routeNumber.isNotBlank(), "Route number should not be blank")
            assertTrue(route.name.isNotBlank(), "Route name should not be blank")
            assertTrue(route.description.isNotBlank(), "Route description should not be blank")
        }
    }
}

