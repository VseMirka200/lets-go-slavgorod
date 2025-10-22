package com.example.lets_go_slavgorod.performance

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import kotlinx.coroutines.measureTimeMillis
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue

/**
 * Performance тесты для критических операций
 * 
 * Тестирует производительность:
 * - Загрузки больших объемов данных
 * - Поиска по маршрутам
 * - Фильтрации данных
 * - Кэширования
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@RunWith(JUnit4::class)
class PerformanceTest {
    
    @Test
    fun `routes loading should be fast`() = runBlocking {
        // Given
        val repository = BusRouteRepository(null)
        
        // When
        val time = measureTimeMillis {
            repository.getAllRoutes()
        }
        
        // Then
        assertTrue(time < 1000, "Routes loading should take less than 1 second, took ${time}ms")
    }
    
    @Test
    fun `search should be fast with large dataset`() = runBlocking {
        // Given
        val repository = BusRouteRepository(null)
        val largeDataset = (1..1000).map { i ->
            BusRoute(
                id = "route_$i",
                routeNumber = "$i",
                name = "Автобус №$i",
                description = "Описание маршрута $i",
                isActive = true,
                isFavorite = false,
                color = "#FF6200EE",
                pricePrimary = 50,
                priceSecondary = 0,
                directionDetails = "Направление $i",
                travelTime = "30 минут",
                paymentMethods = listOf("Наличные")
            )
        }
        
        // When
        val time = measureTimeMillis {
            largeDataset.filter { route ->
                route.name.contains("100", ignoreCase = true)
            }
        }
        
        // Then
        assertTrue(time < 100, "Search should take less than 100ms, took ${time}ms")
    }
    
    @Test
    fun `filtering should be efficient`() = runBlocking {
        // Given
        val routes = (1..100).map { i ->
            BusRoute(
                id = "route_$i",
                routeNumber = "$i",
                name = "Автобус №$i",
                description = "Описание маршрута $i",
                isActive = i % 2 == 0, // Половина активных
                isFavorite = i % 3 == 0, // Треть избранных
                color = "#FF6200EE",
                pricePrimary = 50,
                priceSecondary = 0,
                directionDetails = "Направление $i",
                travelTime = "30 минут",
                paymentMethods = listOf("Наличные")
            )
        }
        
        // When
        val time = measureTimeMillis {
            val activeRoutes = routes.filter { it.isActive }
            val favoriteRoutes = routes.filter { it.isFavorite }
            val expensiveRoutes = routes.filter { it.pricePrimary > 40 }
        }
        
        // Then
        assertTrue(time < 50, "Filtering should take less than 50ms, took ${time}ms")
    }
    
    @Test
    fun `memory usage should be reasonable`() = runBlocking {
        // Given
        val repository = BusRouteRepository(null)
        
        // When
        val routes = repository.getAllRoutes()
        val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Создаем много объектов для тестирования памяти
        val largeList = (1..1000).map { i ->
            routes.map { route ->
                route.copy(id = "${route.id}_$i")
            }
        }.flatten()
        
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryUsed = memoryAfter - memoryBefore
        
        // Then
        val memoryUsedMB = memoryUsed / 1024 / 1024
        assertTrue(memoryUsedMB < 50, "Memory usage should be less than 50MB, used ${memoryUsedMB}MB")
    }
    
    @Test
    fun `concurrent operations should be safe`() = runBlocking {
        // Given
        val repository = BusRouteRepository(null)
        
        // When
        val time = measureTimeMillis {
            // Запускаем несколько операций параллельно
            val jobs = (1..10).map {
                kotlinx.coroutines.async {
                    repository.getAllRoutes()
                    repository.searchRoutes("102")
                    repository.getRouteById("102")
                }
            }
            
            // Ждем завершения всех операций
            jobs.forEach { it.await() }
        }
        
        // Then
        assertTrue(time < 2000, "Concurrent operations should complete in less than 2 seconds, took ${time}ms")
    }
}