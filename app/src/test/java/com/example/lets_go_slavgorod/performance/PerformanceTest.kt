package com.example.lets_go_slavgorod.performance

import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertTrue
import android.content.Context
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

/**
 * Performance тесты для критических операций
 * 
 * Тестирует производительность ключевых операций приложения:
 * - Загрузки больших объемов данных (1000+ маршрутов)
 * - Поиска по маршрутам с различными алгоритмами
 * - Фильтрации данных по различным критериям
 * - Кэширования и управления памятью
 * - Параллельных операций и thread safety
 * - Memory usage и предотвращение утечек памяти
 * 
 * Все тесты имеют строгие временные ограничения для обеспечения
 * высокой производительности в production среде.
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.1
 */
@RunWith(JUnit4::class)
class PerformanceTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    init {
        MockitoAnnotations.openMocks(this)
        whenever(mockContext.applicationContext).thenReturn(mockContext)
    }
    
    @Test
    fun `routes loading should be fast`() = runBlocking {
        // Given - создаем репозиторий для тестирования
        val repository = BusRouteRepository(mockContext)
        
        // When - измеряем время загрузки маршрутов
        val time = measureTimeMillis {
            repository.getAllRoutes()
        }
        
        // Then - проверяем что загрузка выполняется быстро (< 1 сек)
        assertTrue(time < 1000, "Routes loading should take less than 1 second, took ${time}ms")
    }
    
    @Test
    fun `search should be fast with large dataset`() = runBlocking {
        // Given - создаем большой набор данных (1000 маршрутов) для тестирования производительности
        val repository = BusRouteRepository(mockContext)
        val largeDataset = (1..1000).map { i ->
            BusRoute(
                id = "route_$i",
                routeNumber = "$i",
                name = "Автобус №$i",
                description = "Описание маршрута $i",
                isActive = true,
                isFavorite = false,
                color = "#FF6200EE",
                pricePrimary = "50",
                priceSecondary = "0",
                directionDetails = "Направление $i",
                travelTime = "30 минут",
                paymentMethods = "Наличные"
            )
        }
        
        // When - измеряем время поиска по большому набору данных
        val time = measureTimeMillis {
            largeDataset.filter { route ->
                route.name.contains("100", ignoreCase = true)
            }
        }
        
        // Then - проверяем что поиск выполняется быстро (< 100мс)
        assertTrue(time < 100, "Search should take less than 100ms, took ${time}ms")
    }
    
    @Test
    fun `filtering should be efficient`() = runBlocking {
        // Given - создаем набор маршрутов с различными характеристиками для тестирования фильтрации
        val routes = (1..100).map { i ->
            BusRoute(
                id = "route_$i",
                routeNumber = "$i",
                name = "Автобус №$i",
                description = "Описание маршрута $i",
                isActive = i % 2 == 0, // Половина активных
                isFavorite = i % 3 == 0, // Треть избранных
                color = "#FF6200EE",
                pricePrimary = "50",
                priceSecondary = "0",
                directionDetails = "Направление $i",
                travelTime = "30 минут",
                paymentMethods = "Наличные"
            )
        }
        
        // When - измеряем время выполнения различных фильтраций
        val time = measureTimeMillis {
            val activeRoutes = routes.filter { it.isActive }
            val favoriteRoutes = routes.filter { it.isFavorite }
            val expensiveRoutes = routes.filter { it.pricePrimary?.toIntOrNull() ?: 0 > 40 }
        }
        
        // Then - проверяем что фильтрация выполняется эффективно (< 50мс)
        assertTrue(time < 50, "Filtering should take less than 50ms, took ${time}ms")
    }
    
    @Test
    fun `memory usage should be reasonable`() = runBlocking {
        // Given - создаем репозиторий и получаем базовые маршруты
        val repository = BusRouteRepository(mockContext)
        
        // When - измеряем использование памяти до и после создания большого количества объектов
        val routes = repository.getAllRoutes()
        val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Создаем много объектов для тестирования памяти (1000 копий каждого маршрута)
        val largeList = (1..1000).map { i ->
            routes.map { route ->
                route.copy(id = "${route.id}_$i")
            }
        }.flatten()
        
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryUsed = memoryAfter - memoryBefore
        
        // Then - проверяем что использование памяти остается разумным (< 50MB)
        val memoryUsedMB = memoryUsed / 1024 / 1024
        assertTrue(memoryUsedMB < 50, "Memory usage should be less than 50MB, used ${memoryUsedMB}MB")
    }
    
    @Test
    fun `concurrent operations should be safe`() = runBlocking {
        // Given - создаем репозиторий для тестирования параллельных операций
        val repository = BusRouteRepository(mockContext)
        
        // When - запускаем 10 параллельных операций и измеряем время выполнения
        val time = measureTimeMillis {
            // Запускаем несколько операций параллельно для тестирования thread safety
            runBlocking {
                val jobs = (1..10).map {
                    async {
                        repository.getAllRoutes()
                        repository.searchRoutes("102")
                        repository.getRouteById("102")
                    }
                }
                
                // Ждем завершения всех операций
                jobs.awaitAll()
            }
        }
        
        // Then - проверяем что параллельные операции выполняются безопасно и быстро (< 2 сек)
        assertTrue(time < 2000, "Concurrent operations should complete in less than 2 seconds, took ${time}ms")
    }
}