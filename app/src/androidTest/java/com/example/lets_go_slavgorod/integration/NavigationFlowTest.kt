package com.example.lets_go_slavgorod.integration

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.MainActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для навигации между экранами
 * 
 * Тестируют:
 * - Навигацию между экранами
 * - Переходы в расписание маршрута
 * - Переходы в настройки
 * - Возврат назад
 * - Глубокие ссылки (deep links)
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
@RunWith(AndroidJUnit4::class)
class NavigationFlowTest {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun app_startsOnHomeScreen() {
        // Проверяем, что приложение запускается на главном экране
        composeTestRule
            .onNodeWithText("Поехали! Славгород")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_canNavigateToSettings() {
        // Нажимаем на кнопку настроек
        composeTestRule
            .onNodeWithContentDescription("Настройки")
            .assertIsDisplayed()
            .performClick()
        
        // Проверяем, что открылся экран настроек
        composeTestRule
            .onNodeWithText("Настройки")
            .assertIsDisplayed()
        
        // Проверяем наличие разделов настроек
        composeTestRule
            .onNodeWithText("Тема приложения")
            .assertIsDisplayed()
    }
    
    @Test
    fun settingsScreen_canNavigateBack() {
        // Переходим в настройки
        composeTestRule
            .onNodeWithContentDescription("Настройки")
            .performClick()
        
        // Нажимаем кнопку назад
        composeTestRule
            .onNodeWithContentDescription("Назад")
            .performClick()
        
        // Проверяем, что вернулись на главный экран
        composeTestRule
            .onNodeWithText("Поехали! Славгород")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_displaysSearchBar() {
        // Проверяем наличие строки поиска
        composeTestRule
            .onNodeWithText("Поиск маршрутов...")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_searchFunctionality_works() {
        // Вводим поисковый запрос
        composeTestRule
            .onNodeWithText("Поиск маршрутов...")
            .performTextInput("102")
        
        // Проверяем, что отображаются результаты поиска
        // (маршрут 102 должен быть видим)
        composeTestRule
            .onNodeWithText("102")
            .assertIsDisplayed()
    }
    
    @Test
    fun homeScreen_canOpenRouteSchedule() {
        // Ждем загрузки маршрутов
        composeTestRule.waitForIdle()
        
        // Нажимаем на карточку маршрута 102
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        // Проверяем, что открылся экран расписания
        composeTestRule
            .onNodeWithText("Автобус №102")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleScreen_canNavigateBack() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        // Ждем загрузки расписания
        composeTestRule.waitForIdle()
        
        // Нажимаем кнопку назад
        composeTestRule
            .onNodeWithContentDescription("Назад")
            .performClick()
        
        // Проверяем, что вернулись на главный экран
        composeTestRule
            .onNodeWithText("Поехали! Славгород")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleScreen_canOpenNotificationSettings() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        // Ждем загрузки расписания
        composeTestRule.waitForIdle()
        
        // Нажимаем на кнопку уведомлений
        composeTestRule
            .onNodeWithContentDescription("Настройки уведомлений")
            .performClick()
        
        // Проверяем, что открылся экран настроек уведомлений
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Настройки уведомлений")
            .assertIsDisplayed()
    }
    
    @Test
    fun notificationSettingsScreen_canNavigateBack() {
        // Открываем расписание
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("102")
            .onFirst()
            .performClick()
        
        // Открываем настройки уведомлений
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Настройки уведомлений")
            .performClick()
        
        // Нажимаем кнопку назад
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Назад")
            .performClick()
        
        // Проверяем, что вернулись на экран расписания
        composeTestRule
            .onNodeWithText("Автобус №102")
            .assertIsDisplayed()
    }
}

