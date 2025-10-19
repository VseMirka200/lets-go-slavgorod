package com.example.lets_go_slavgorod.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit-тесты для BusViewModel
 * 
 * Проверяет корректность работы ViewModel:
 * - Загрузка маршрутов
 * - Поиск по маршрутам
 * - Управление состоянием UI
 * - Обработка ошибок
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.0
 */
@ExperimentalCoroutinesApi
class BusViewModelTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Mock
    private lateinit var mockApplication: Application
    
    private lateinit var viewModel: BusViewModel
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        // BusViewModel теперь создается с реальной логикой, но с mock Application
        // Для полноценного тестирования нужен instrumented test, но базовые проверки можем сделать
        `when`(mockApplication.applicationContext).thenReturn(mockApplication)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    /**
     * ПРИМЕЧАНИЕ: Эти тесты упрощены для unit-тестирования.
     * Для полного покрытия используйте instrumented tests (androidTest).
     * BusViewModel зависит от реальных компонентов Android (AppDatabase, Context),
     * поэтому полноценное тестирование возможно только через Robolectric или AndroidTest.
     */
    
    @Test
    fun `BusUiState has correct default values`() {
        // Given
        val defaultState = com.example.lets_go_slavgorod.ui.viewmodel.BusUiState()
        
        // Then
        assertTrue(defaultState.routes.isEmpty())
        assertFalse(defaultState.isLoading)
        assertEquals(null, defaultState.error)
        assertFalse(defaultState.isAddingFavorite)
        assertFalse(defaultState.isRemovingFavorite)
    }
    
    @Test
    fun `BusUiState with loading is correct`() {
        // Given
        val loadingState = com.example.lets_go_slavgorod.ui.viewmodel.BusUiState(isLoading = true)
        
        // Then
        assertTrue(loadingState.isLoading)
        assertTrue(loadingState.routes.isEmpty())
    }
    
    @Test
    fun `BusUiState with error is correct`() {
        // Given
        val errorMessage = "Test error"
        val errorState = com.example.lets_go_slavgorod.ui.viewmodel.BusUiState(error = errorMessage)
        
        // Then
        assertEquals(errorMessage, errorState.error)
        assertFalse(errorState.isLoading)
    }
    
    @Test
    fun `BusUiState with routes is correct`() {
        // Given
        val testRoutes = listOf(
            BusRoute(
                id = "102",
                routeNumber = "102",
                name = "Автобус №102",
                description = "Рынок - Яровое",
                travelTime = "~40 минут",
                pricePrimary = "38₽",
                paymentMethods = "Нал./Безнал.",
                color = "#FF6B6BFF"
            )
        )
        val routesState = com.example.lets_go_slavgorod.ui.viewmodel.BusUiState(routes = testRoutes)
        
        // Then
        assertEquals(1, routesState.routes.size)
        assertEquals("102", routesState.routes[0].id)
        assertFalse(routesState.isLoading)
    }
}