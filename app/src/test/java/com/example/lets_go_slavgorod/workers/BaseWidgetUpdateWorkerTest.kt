package com.example.lets_go_slavgorod.workers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.lets_go_slavgorod.widget.BaseRouteWidgetProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit тесты для BaseWidgetUpdateWorker
 * 
 * Тестирует:
 * - Выполнение работы
 * - Обработку параметров
 * - Обработку ошибок
 * - Результаты работы
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
@RunWith(JUnit4::class)
class BaseWidgetUpdateWorkerTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockWorkerParameters: WorkerParameters
    
    private lateinit var context: Context
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        // Инициализация WorkManager для тестов
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }
    
    @Test
    fun `doWork returns success for valid route id`() {
        // Given
        val routeId = "102"
        whenever(mockWorkerParameters.inputData.getString("route_id")).thenReturn(routeId)
        
        val worker = BaseRouteWidgetProvider.BaseWidgetUpdateWorker(context, mockWorkerParameters)
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(ListenableWorker.Result.success(), result)
    }
    
    @Test
    fun `doWork returns failure for invalid route id`() {
        // Given
        whenever(mockWorkerParameters.inputData.getString("route_id")).thenReturn(null)
        
        val worker = BaseRouteWidgetProvider.BaseWidgetUpdateWorker(context, mockWorkerParameters)
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }
    
    @Test
    fun `doWork returns failure for unknown route id`() {
        // Given
        val unknownRouteId = "999"
        whenever(mockWorkerParameters.inputData.getString("route_id")).thenReturn(unknownRouteId)
        
        val worker = BaseRouteWidgetProvider.BaseWidgetUpdateWorker(context, mockWorkerParameters)
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }
    
    @Test
    fun `worker handles exceptions gracefully`() {
        // Given
        val routeId = "102"
        whenever(mockWorkerParameters.inputData.getString("route_id")).thenReturn(routeId)
        whenever(mockContext.applicationContext).thenThrow(RuntimeException("Test exception"))
        
        val worker = BaseRouteWidgetProvider.BaseWidgetUpdateWorker(mockContext, mockWorkerParameters)
        
        // When
        val result = worker.doWork()
        
        // Then
        assertEquals(ListenableWorker.Result.failure(), result)
    }
}
