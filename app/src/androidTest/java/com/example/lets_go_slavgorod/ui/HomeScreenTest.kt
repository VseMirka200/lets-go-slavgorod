package com.example.lets_go_slavgorod.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.screens.EmptyState
import com.example.lets_go_slavgorod.ui.screens.ErrorState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для HomeScreen
 * 
 * Тестируют отображение и взаимодействие с главным экраном приложения.
 * 
 * @author VseMirka200
 * @version 1.0
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun emptyState_displaysCorrectMessageWhenSearchQueryEmpty() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "")
        }
        
        composeTestRule
            .onNodeWithText("Маршруты не найдены")
            .assertIsDisplayed()
    }
    
    @Test
    fun emptyState_displaysCorrectMessageWhenSearchQueryNotEmpty() {
        val searchQuery = "102"
        composeTestRule.setContent {
            EmptyState(searchQuery = searchQuery)
        }
        
        composeTestRule
            .onNodeWithText("Ничего не найдено")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("По запросу \"$searchQuery\" не найдено маршрутов.\nПопробуйте изменить поисковый запрос.")
            .assertIsDisplayed()
    }
    
    @Test
    fun errorState_displaysErrorMessage() {
        val errorMessage = "Тестовая ошибка"
        composeTestRule.setContent {
            ErrorState(errorMessage = errorMessage)
        }
        
        composeTestRule
            .onNodeWithText(errorMessage)
            .assertIsDisplayed()
    }
    
    @Test
    fun emptyState_showsPullToRefreshHintWhenNoSearchQuery() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "")
        }
        
        composeTestRule
            .onNodeWithText("Потяните экран вниз для обновления")
            .assertIsDisplayed()
    }
    
    @Test
    fun emptyState_doesNotShowPullToRefreshHintWhenSearchQueryExists() {
        composeTestRule.setContent {
            EmptyState(searchQuery = "102")
        }
        
        composeTestRule
            .onNodeWithText("Потяните экран вниз для обновления")
            .assertDoesNotExist()
    }
}

