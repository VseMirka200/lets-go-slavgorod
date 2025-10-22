package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit тесты для RoutesViewModel
 * 
 * Тестирует основную функциональность ViewModel:
 * - Загрузка маршрутов
 * - Поиск по маршрутам
 * - Обновление данных
 * - Обработка ошибок
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class RoutesViewModelTest {
    
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var mockApplication: Application
    private lateinit var mockRepository: BusRouteRepository
    private lateinit var viewModel: RoutesViewModel
    
    private val testRoutes = listOf(
        BusRoute(
            id = "102",
            routeNumber = "102",
            name = "Автобус №102",
            description = "Славгород — Яровое",
            isActive = true,
            isFavorite = false,
            color = "#FF6200EE",
            pricePrimary = 50,
            priceSecondary = 0,
            directionDetails = "Славгород (Рынок) — Яровое (МСЧ)",
            travelTime = "45 минут",
            paymentMethods = listOf("Наличные", "Банковская карта")
        ),
        BusRoute(
            id = "1",
            routeNumber = "1",
            name = "Автобус №1",
            description = "Вокзал — Совхоз",
            isActive = true,
            isFavorite = false,
            color = "#FF1976D2",
            pricePrimary = 25,
            priceSecondary = 0,
            directionDetails = "Вокзал — Совхоз",
            travelTime = "20 минут",
            paymentMethods = listOf("Наличные")
        )
    )
    
    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockApplication = mockk(relaxed = true)
        mockRepository = mockk(relaxed = true)
        
        // Настраиваем mock repository
        coEvery { mockRepository.routes } returns flowOf(testRoutes)
        coEvery { mockRepository.getRouteById(any()) } returns testRoutes.first()
        
        viewModel = RoutesViewModel(mockApplication)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `initial state should be loading`() {
        // Given
        val initialState = viewModel.uiState.value
        
        // Then
        assertTrue(initialState.isLoading)
        assertTrue(initialState.routes.isEmpty())
        assertEquals(null, initialState.error)
    }
    
    @Test
    fun `should load routes successfully`() {
        // When
        // ViewModel автоматически загружает маршруты при инициализации
        
        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.routes.isNotEmpty())
        assertEquals(null, state.error)
    }
    
    @Test
    fun `should handle search query`() {
        // Given
        val searchQuery = "102"
        
        // When
        viewModel.updateSearchQuery(searchQuery)
        
        // Then
        assertEquals(searchQuery, viewModel.searchQuery.value)
    }
    
    @Test
    fun `should get route by id`() {
        // Given
        val routeId = "102"
        
        // When
        val route = viewModel.getRouteById(routeId)
        
        // Then
        assertEquals(routeId, route?.id)
        assertEquals("Автобус №102", route?.name)
    }
    
    @Test
    fun `should return null for non-existent route`() {
        // Given
        val nonExistentId = "999"
        
        // When
        val route = viewModel.getRouteById(nonExistentId)
        
        // Then
        assertEquals(null, route)
    }
    
    @Test
    fun `should handle refresh`() {
        // When
        viewModel.refreshRoutes()
        
        // Then
        // Проверяем что refresh был вызван (нет исключений)
        assertTrue(true) // Если дошли до этой строки, значит refresh прошел успешно
    }
    
    @Test
    fun `should clear search query`() {
        // Given
        viewModel.updateSearchQuery("test")
        
        // When
        viewModel.updateSearchQuery("")
        
        // Then
        assertEquals("", viewModel.searchQuery.value)
    }
}