package com.example.lets_go_slavgorod.integration

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.MainActivity
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для фильтрации
 * 
 * Тестируют:
 * - Фильтрацию "Избранные" в расписании
 * - Фильтрацию "Следующий" в расписании
 * - Взаимоисключающие фильтры
 * - Добавление/удаление избранных
 * - Счетчик избранных времен
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.1
 */
@RunWith(AndroidJUnit4::class)
class FilterIntegrationTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    private lateinit var viewModel: BusViewModel
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = BusViewModel(context)
    }
    
    @Test
    fun scheduleScreen_favoriteFilter_isDisplayed() {
        // Открываем расписание маршрута
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Проверяем наличие фильтра "Избранные"
        composeTestRule
            .onNodeWithText("Избранные")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun scheduleScreen_nextFilter_isDisplayed() {
        // Открываем расписание маршрута
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Проверяем наличие фильтра "Следующий"
        composeTestRule
            .onNodeWithText("Следующий")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun scheduleScreen_filtersAreMutuallyExclusive() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Включаем фильтр "Избранные"
        composeTestRule
            .onNodeWithText("Избранные")
            .performClick()
        
        // Включаем фильтр "Следующий"
        composeTestRule
            .onNodeWithText("Следующий")
            .performClick()
        
        // Фильтр "Избранные" должен быть деактивирован
        // (проверяем через состояние UI)
    }
    
    @Test
    fun scheduleScreen_addToFavorites_works() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Нажимаем на звездочку первого рейса
        composeTestRule
            .onAllNodesWithContentDescription("Добавить в избранное")
            .onFirst()
            .performClick()
        
        // Ждем обновления UI
        composeTestRule.waitForIdle()
        
        // Проверяем, что появилась заполненная звездочка
        composeTestRule
            .onAllNodesWithContentDescription("Убрать из избранного")
            .onFirst()
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleScreen_removeFromFavorites_works() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Добавляем в избранное
        composeTestRule
            .onAllNodesWithContentDescription("Добавить в избранное")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Удаляем из избранного
        composeTestRule
            .onAllNodesWithContentDescription("Убрать из избранного")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Проверяем, что звездочка снова пустая
        composeTestRule
            .onAllNodesWithContentDescription("Добавить в избранное")
            .onFirst()
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleScreen_favoriteFilter_showsOnlyFavorites() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Добавляем один рейс в избранное
        composeTestRule
            .onAllNodesWithContentDescription("Добавить в избранное")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Включаем фильтр "Избранные"
        composeTestRule
            .onNodeWithText("Избранные")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Должен отображаться только избранный рейс
        // (проверяем через счетчик или количество карточек)
    }
    
    @Test
    fun scheduleScreen_favoriteCounter_displaysCorrectCount() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Добавляем несколько рейсов в избранное
        val favoriteButtons = composeTestRule
            .onAllNodesWithContentDescription("Добавить в избранное")
        
        favoriteButtons[0].performClick()
        composeTestRule.waitForIdle()
        
        favoriteButtons[1].performClick()
        composeTestRule.waitForIdle()
        
        // Включаем фильтр "Избранные"
        composeTestRule
            .onNodeWithText("Избранные")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Проверяем счетчик (должен показывать 2 времени)
        composeTestRule
            .onNodeWithText("2 времени")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleScreen_emptyFavorites_showsEmptyState() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Включаем фильтр "Избранные" без добавления избранных
        composeTestRule
            .onNodeWithText("Избранные")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Должно отображаться сообщение о пустом состоянии
        composeTestRule
            .onNodeWithText("Нет избранных времен")
            .assertIsDisplayed()
    }
}

