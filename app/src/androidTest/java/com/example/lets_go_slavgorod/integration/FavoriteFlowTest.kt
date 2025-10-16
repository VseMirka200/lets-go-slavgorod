package com.example.lets_go_slavgorod.integration

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * Интеграционные тесты для базы данных избранных времен
 * 
 * Тестируют операции с избранными временами в базе данных:
 * - Добавление избранного времени
 * - Удаление избранного времени  
 * - Обновление статуса активности
 * - Работа с несколькими избранными
 * 
 * Примечание: Избранные времена используются для настройки уведомлений маршрутов.
 * Пользователь добавляет время в избранное через экран расписания (звёздочка),
 * настраивает уведомления через кнопку в шапке расписания.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
@RunWith(AndroidJUnit4::class)
class FavoriteFlowTest {
    
    private lateinit var database: AppDatabase
    private lateinit var viewModel: BusViewModel
    private lateinit var context: Context
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        
        // Создаем in-memory базу данных для тестов
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        // Создаем ViewModel
        viewModel = BusViewModel(context as Application)
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun addFavorite_savesToDatabase() = runTest {
        // Arrange
        val scheduleId = "102_slav_1"
        val favoriteEntity = FavoriteTimeEntity(
            id = scheduleId,
            routeId = "102",
            routeNumber = "102",
            routeName = "Автобус №102",
            departureTime = "08:00",
            stopName = "Рынок (Славгород)",
            departurePoint = "Рынок (Славгород)",
            dayOfWeek = "Будни",
            addedDate = System.currentTimeMillis(),
            isActive = true
        )
        
        // Act
        database.favoriteTimeDao().addFavoriteTime(favoriteEntity)
        
        // Assert
        val favorites = database.favoriteTimeDao().getAllFavoriteTimes().first()
        assertTrue(favorites.any { it.id == scheduleId })
        assertEquals(1, favorites.size)
    }
    
    @Test
    fun removeFavorite_removesFromDatabase() = runTest {
        // Arrange
        val scheduleId = "102_slav_1"
        val favoriteEntity = FavoriteTimeEntity(
            id = scheduleId,
            routeId = "102",
            routeNumber = "102",
            routeName = "Автобус №102",
            departureTime = "08:00",
            stopName = "Рынок (Славгород)",
            departurePoint = "Рынок (Славгород)",
            dayOfWeek = "Будни",
            addedDate = System.currentTimeMillis(),
            isActive = true
        )
        database.favoriteTimeDao().addFavoriteTime(favoriteEntity)
        
        // Act
        database.favoriteTimeDao().removeFavoriteTime(scheduleId)
        
        // Assert
        val favorites = database.favoriteTimeDao().getAllFavoriteTimes().first()
        assertTrue(favorites.none { it.id == scheduleId })
        assertEquals(0, favorites.size)
    }
    
    @Test
    fun updateFavoriteActiveState_updatesInDatabase() = runTest {
        // Arrange
        val scheduleId = "102_slav_1"
        val favoriteEntity = FavoriteTimeEntity(
            id = scheduleId,
            routeId = "102",
            routeNumber = "102",
            routeName = "Автобус №102",
            departureTime = "08:00",
            stopName = "Рынок (Славгород)",
            departurePoint = "Рынок (Славгород)",
            dayOfWeek = "Будни",
            addedDate = System.currentTimeMillis(),
            isActive = true
        )
        database.favoriteTimeDao().addFavoriteTime(favoriteEntity)
        
        // Act
        database.favoriteTimeDao().updateFavoriteTime(favoriteEntity.copy(isActive = false))
        
        // Assert
        val favorite = database.favoriteTimeDao().getFavoriteTimeById(scheduleId).first().first()
        assertFalse(favorite.isActive)
    }
    
    @Test
    fun multipleFavorites_canBeAddedAndRetrieved() = runTest {
        // Arrange
        val favorites = listOf(
            FavoriteTimeEntity(
                id = "102_slav_1",
                routeId = "102",
                routeNumber = "102",
                routeName = "Автобус №102",
                departureTime = "08:00",
                stopName = "Рынок (Славгород)",
                departurePoint = "Рынок (Славгород)",
                dayOfWeek = "Будни",
                addedDate = System.currentTimeMillis(),
                isActive = true
            ),
            FavoriteTimeEntity(
                id = "102B_slav_1",
                routeId = "102B",
                routeNumber = "102Б",
                routeName = "Автобус 102Б",
                departureTime = "09:00",
                stopName = "Рынок (Славгород)",
                departurePoint = "Рынок (Славгород)",
                dayOfWeek = "Будни",
                addedDate = System.currentTimeMillis(),
                isActive = true
            )
        )
        
        // Act
        favorites.forEach { database.favoriteTimeDao().addFavoriteTime(it) }
        
        // Assert
        val savedFavorites = database.favoriteTimeDao().getAllFavoriteTimes().first()
        assertEquals(2, savedFavorites.size)
        assertTrue(savedFavorites.any { it.routeId == "102" })
        assertTrue(savedFavorites.any { it.routeId == "102B" })
    }
}

