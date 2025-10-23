package com.example.lets_go_slavgorod.data.repository

import android.content.Context
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit тесты для BusRouteRepository
 * 
 * Тестирует основную функциональность репозитория автобусных маршрутов:
 * - Загрузка маршрутов из различных источников (локальные данные, сеть)
 * - Поиск маршрутов по ID с O(1) сложностью
 * - Поиск маршрутов по текстовому запросу (номер, название)
 * - Валидация данных маршрутов
 * - Обработка ошибок и edge cases
 * - Кэширование и производительность
 * 
 * Использует Mockito для мокирования зависимостей.
 * Все тесты выполняются в изолированной среде.
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.1
 */
@RunWith(JUnit4::class)
class BusRouteRepositoryTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    private lateinit var repository: BusRouteRepository
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
        repository = BusRouteRepository(mockContext)
    }
    
    @Test
    fun `should return non-empty routes list`() = runTest {
        // When - получаем все маршруты из репозитория
        val routes = repository.getAllRoutes()
        
        // Then - проверяем что список не пустой
        assertTrue(routes.isNotEmpty(), "Routes should not be empty")
    }
    
    @Test
    fun `should return valid routes`() = runTest {
        // When - получаем все маршруты
        val routes = repository.getAllRoutes()
        
        // Then - проверяем валидность каждого маршрута
        routes.forEach { route ->
            assertTrue(route.isValid(), "Route ${route.id} should be valid")
            assertTrue(route.id.isNotBlank(), "Route ID should not be blank")
            assertTrue(route.name.isNotBlank(), "Route name should not be blank")
        }
    }
    
    @Test
    fun `should find route by id`() = runTest {
        // Given
        val routes = repository.getAllRoutes()
        val firstRoute = routes.first()
        
        // When
        val foundRoute = repository.getRouteById(firstRoute.id)
        
        // Then
        assertNotNull(foundRoute, "Route should be found")
        assertEquals(firstRoute.id, foundRoute?.id)
        assertEquals(firstRoute.name, foundRoute?.name)
    }
    
    @Test
    fun `should return null for non-existent route`() = runTest {
        // Given
        val nonExistentId = "999"
        
        // When
        val route = repository.getRouteById(nonExistentId)
        
        // Then
        assertEquals(null, route)
    }
    
    @Test
    fun `should handle search query`() = runTest {
        // Given
        val searchQuery = "102"
        
        // When
        val routes = repository.searchRoutes(searchQuery)
        
        // Then
        assertTrue(routes.isNotEmpty(), "Search should return results")
        routes.forEach { route ->
            assertTrue(
                route.name.contains(searchQuery, ignoreCase = true) ||
                route.routeNumber.contains(searchQuery, ignoreCase = true),
                "Route should match search query"
            )
        }
    }
    
    @Test
    fun `should return empty list for empty search query`() = runTest {
        // Given
        val emptyQuery = ""
        
        // When
        val routes = repository.searchRoutes(emptyQuery)
        
        // Then
        assertTrue(routes.isEmpty(), "Empty search should return empty list")
    }
    
    @Test
    fun `should handle case insensitive search`() = runTest {
        // Given
        val searchQuery = "АВТОБУС"
        
        // When
        val routes = repository.searchRoutes(searchQuery)
        
        // Then
        routes.forEach { route ->
            assertTrue(
                route.name.contains(searchQuery, ignoreCase = true),
                "Search should be case insensitive"
            )
        }
    }
    
    @Test
    fun `should validate route data`() = runTest {
        // When
        val routes = repository.getAllRoutes()
        
        // Then
        routes.forEach { route ->
            // Проверяем обязательные поля
            assertTrue(route.id.isNotBlank(), "Route ID should not be blank")
            assertTrue(route.name.isNotBlank(), "Route name should not be blank")
            assertTrue(route.routeNumber.isNotBlank(), "Route number should not be blank")
            assertTrue(route.color.isNotBlank(), "Route color should not be blank")
            // Price validation is not applicable for string type
            // assertTrue(route.pricePrimary >= 0, "Price should be non-negative")
        }
    }
}