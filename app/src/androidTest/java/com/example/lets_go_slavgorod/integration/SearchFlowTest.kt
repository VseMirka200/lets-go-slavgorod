package com.example.lets_go_slavgorod.integration

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Интеграционные тесты для поиска маршрутов
 * 
 * Тестируют полный цикл поиска маршрутов через ViewModel и Repository.
 * 
 * @author VseMirka200
 * @version 1.0
 */
@RunWith(AndroidJUnit4::class)
class SearchFlowTest {
    
    private lateinit var viewModel: BusViewModel
    private lateinit var repository: BusRouteRepository
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        viewModel = BusViewModel(context as Application)
        repository = BusRouteRepository(context)
    }
    
    @Test
    fun searchByRouteNumber_returnsCorrectRoute() = runTest {
        // Act
        viewModel.onSearchQueryChange("102")
        
        // Даем время на обработку
        kotlinx.coroutines.delay(100)
        
        // Assert
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.any { it.routeNumber == "102" })
    }
    
    @Test
    fun searchByRouteName_returnsMatchingRoutes() = runTest {
        // Act
        viewModel.onSearchQueryChange("Автобус")
        
        // Даем время на обработку
        kotlinx.coroutines.delay(100)
        
        // Assert
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.all { it.name.contains("Автобус", ignoreCase = true) })
    }
    
    @Test
    fun searchWithNonExistentQuery_returnsEmptyList() = runTest {
        // Act
        viewModel.onSearchQueryChange("НесуществующийМаршрут123")
        
        // Даем время на обработку
        kotlinx.coroutines.delay(100)
        
        // Assert
        val uiState = viewModel.uiState.first()
        assertTrue(uiState.routes.isEmpty())
    }
    
    @Test
    fun clearSearch_returnsAllRoutes() = runTest {
        // Arrange - сначала делаем поиск
        viewModel.onSearchQueryChange("102")
        kotlinx.coroutines.delay(100)
        val searchResults = viewModel.uiState.first().routes.size
        
        // Act - очищаем поиск
        viewModel.onSearchQueryChange("")
        kotlinx.coroutines.delay(100)
        
        // Assert
        val allRoutes = viewModel.uiState.first().routes.size
        assertTrue(allRoutes > searchResults)
    }
    
    @Test
    fun repository_getAllRoutes_returnsNonEmptyList() {
        // Act
        val routes = repository.getAllRoutes()
        
        // Assert
        assertTrue(routes.isNotEmpty())
        assertTrue(routes.any { it.id == "102" })
        assertTrue(routes.any { it.id == "102B" })
        assertTrue(routes.any { it.id == "1" })
    }
    
    @Test
    fun repository_getRouteById_returnsCorrectRoute() {
        // Act
        val route = repository.getRouteById("102")
        
        // Assert
        assertNotNull(route)
        assertEquals("102", route?.id)
        assertEquals("102", route?.routeNumber)
    }
}

