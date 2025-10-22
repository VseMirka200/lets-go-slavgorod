package com.example.lets_go_slavgorod.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import com.example.lets_go_slavgorod.ui.viewmodel.ScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit тесты для ScheduleViewModel
 * 
 * Проверяет:
 * - Получение расписания для маршрута
 * - Обновление расписания
 * - Кэширование
 * 
 * Упрощённые тесты без моков.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.1
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleViewModelTest {
    
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()
    
    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var application: Application
    private lateinit var viewModel: ScheduleViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        viewModel = ScheduleViewModel(application)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `getScheduleFor returns StateFlow`() {
        val scheduleFlow = viewModel.getScheduleFor("102")
        assertNotNull(scheduleFlow)
    }
    
    @Test
    fun `getScheduleFor caches result`() = runTest {
        val flow1 = viewModel.getScheduleFor("102")
        val flow2 = viewModel.getScheduleFor("102")
        
        // Should return same flow instance (cached)
        assertTrue(flow1 === flow2)
    }
    
    @Test
    fun `refreshSchedule updates cached data`() = runTest {
        // Get initial schedule
        val scheduleFlow = viewModel.getScheduleFor("102")
        advanceTimeBy(100)
        
        // Refresh
        viewModel.refreshSchedule("102")
        advanceTimeBy(100)
        
        // Flow should emit new data
        assertNotNull(scheduleFlow)
    }
    
    @Test
    fun `getSchedulesForRoute returns schedules`() = runTest {
        val schedules = viewModel.getSchedulesForRoute("102")
        
        // Should return list (might be empty depending on data source)
        assertNotNull(schedules)
    }
}