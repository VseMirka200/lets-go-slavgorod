package com.example.lets_go_slavgorod.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.ui.components.BusRouteCard
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для BusRouteCard
 * 
 * Тестируют отображение карточек маршрутов в разных режимах.
 * 
 * @author VseMirka200
 * @version 1.0
 */
@RunWith(AndroidJUnit4::class)
class BusRouteCardTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private val sampleRoute = BusRoute(
        id = "102",
        routeNumber = "102",
        name = "Автобус №102",
        description = "Рынок — Ст. Зори",
        travelTime = "~40 минут",
        pricePrimary = "38₽ / 55₽",
        paymentMethods = "Нал. / Безнал.",
        color = "#FF6200EE"
    )
    
    @Test
    fun busRouteCard_displaysRouteNumber() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = {}
                )
            }
        }
        
        composeTestRule
            .onNodeWithText(sampleRoute.routeNumber)
            .assertIsDisplayed()
    }
    
    @Test
    fun busRouteCard_isClickable() {
        var clicked = false
        
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = { clicked = true }
                )
            }
        }
        
        composeTestRule
            .onNodeWithContentDescription("Маршрут ${sampleRoute.routeNumber}: ${sampleRoute.name}. Нажмите для просмотра расписания")
            .performClick()
        
        assert(clicked) { "Card was not clicked" }
    }
    
    @Test
    fun busRouteCard_displaysAutobusLabel() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = true,
                    gridColumns = 2,
                    onClick = {}
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("АВТОБУС")
            .assertIsDisplayed()
    }
    
    @Test
    fun busRouteCard_listMode_displaysSeparator() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                BusRouteCard(
                    route = sampleRoute,
                    isGridMode = false,
                    onClick = {}
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("•")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText(sampleRoute.name)
            .assertIsDisplayed()
    }
}

