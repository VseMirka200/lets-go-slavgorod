package com.example.lets_go_slavgorod.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.CompactScheduleCard
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для CompactScheduleCard
 * 
 * Тестируют:
 * - Отображение времени отправления
 * - Отображение порядкового номера
 * - Иконку избранного (звездочка)
 * - Индикатор "Следующий"
 * - Взаимодействие с карточкой
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.1
 */
@RunWith(AndroidJUnit4::class)
class CompactScheduleCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val testSchedule = BusSchedule(
        id = "102_1",
        routeId = "102",
        departureTime = "08:30",
        arrivalTime = "09:10",
        departurePoint = "Рынок (Славгород)",
        arrivalPoint = "Ст. Зори (Яровое)",
        isWorkingDay = true
    )
    
    @Test
    fun compactScheduleCard_displaysDepartureTime() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Проверяем отображение времени
        composeTestRule
            .onNodeWithText("08:30")
            .assertIsDisplayed()
    }
    
    @Test
    fun compactScheduleCard_displaysOrderNumber() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 5
                )
            }
        }
        
        // Проверяем отображение порядкового номера
        composeTestRule
            .onNodeWithText("5.")
            .assertIsDisplayed()
    }
    
    @Test
    fun compactScheduleCard_displaysStarIcon_whenNotFavorite() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Проверяем наличие иконки звездочки
        composeTestRule
            .onNodeWithContentDescription("Добавить в избранное")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun compactScheduleCard_displaysFilledStar_whenFavorite() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = true,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Проверяем наличие заполненной звездочки
        composeTestRule
            .onNodeWithContentDescription("Убрать из избранного")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun compactScheduleCard_displaysNextIndicator_whenIsNextUpcoming() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = true,
                    orderNumber = 1
                )
            }
        }
        
        // Проверяем отображение индикатора "Следующий"
        composeTestRule
            .onNodeWithText("Следующий")
            .assertIsDisplayed()
    }
    
    @Test
    fun compactScheduleCard_doesNotDisplayNextIndicator_whenNotNextUpcoming() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Проверяем отсутствие индикатора "Следующий"
        composeTestRule
            .onNodeWithText("Следующий")
            .assertDoesNotExist()
    }
    
    @Test
    fun compactScheduleCard_isClickable() {
        var clicked = false
        
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = {},
                    onCardClick = { clicked = true },
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Нажимаем на карточку
        composeTestRule
            .onNodeWithText("08:30")
            .performClick()
        
        // Проверяем, что обработчик был вызван
        assert(clicked) { "Card click was not handled" }
    }
    
    @Test
    fun compactScheduleCard_favoriteButton_isClickable() {
        var favoriteClicked = false
        
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = false,
                    onFavoriteClick = { favoriteClicked = true },
                    onCardClick = {},
                    isNextUpcoming = false,
                    orderNumber = 1
                )
            }
        }
        
        // Нажимаем на иконку избранного
        composeTestRule
            .onNodeWithContentDescription("Добавить в избранное")
            .performClick()
        
        // Проверяем, что обработчик был вызван
        assert(favoriteClicked) { "Favorite button click was not handled" }
    }
    
    @Test
    fun compactScheduleCard_displaysAllElements_whenFullyConfigured() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                CompactScheduleCard(
                    schedule = testSchedule,
                    isFavorite = true,
                    onFavoriteClick = {},
                    onCardClick = {},
                    isNextUpcoming = true,
                    orderNumber = 7
                )
            }
        }
        
        // Проверяем все элементы одновременно
        composeTestRule
            .onNodeWithText("7.")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("08:30")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Следующий")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithContentDescription("Убрать из избранного")
            .assertIsDisplayed()
    }
}

