package com.example.lets_go_slavgorod.ui

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.BusSchedule
import com.example.lets_go_slavgorod.ui.components.schedule.ScheduleList
import com.example.lets_go_slavgorod.ui.theme.lets_go_slavgorodTheme
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для ScheduleScreen и компонентов расписания
 * 
 * Тестируют:
 * - Отображение расписаний
 * - Фильтрацию (Избранные, Следующий)
 * - Взаимодействие с карточками
 * - Сворачивание секций (маршрут №1)
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
@RunWith(AndroidJUnit4::class)
class ScheduleScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var viewModel: BusViewModel
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = BusViewModel(context)
    }
    
    private val testRoute = BusRoute(
        id = "102",
        routeNumber = "102",
        name = "Автобус №102",
        description = "Рынок (Славгород) — Ст. Зори (Яровое)",
        travelTime = "~40 минут",
        pricePrimary = "38₽ город / 55₽ межгород",
        paymentMethods = "Нал. / Безнал.",
        color = "#FF5722"
    )
    
    private val testSchedules = listOf(
        BusSchedule(
            id = "102_1",
            routeId = "102",
            departureTime = "08:30",
            arrivalTime = "09:10",
            departurePoint = "Рынок (Славгород)",
            arrivalPoint = "Ст. Зори (Яровое)",
            isWorkingDay = true
        ),
        BusSchedule(
            id = "102_2",
            routeId = "102",
            departureTime = "10:00",
            arrivalTime = "10:40",
            departurePoint = "Рынок (Славгород)",
            arrivalPoint = "Ст. Зори (Яровое)",
            isWorkingDay = true
        ),
        BusSchedule(
            id = "102_3",
            routeId = "102",
            departureTime = "14:30",
            arrivalTime = "15:10",
            departurePoint = "Ст. Зори (Яровое)",
            arrivalPoint = "Рынок (Славгород)",
            isWorkingDay = true
        )
    )
    
    @Test
    fun scheduleList_displaysRouteInformation() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules.take(2),
                    schedulesYarovoe = testSchedules.drop(2),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем отображение названия маршрута
        composeTestRule
            .onNodeWithText("Автобус №102")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleList_displaysFilterButtons() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules,
                    schedulesYarovoe = emptyList(),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем наличие кнопок фильтров
        composeTestRule
            .onNodeWithText("Избранные")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Следующий")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleList_filterButtonsAreClickable() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules,
                    schedulesYarovoe = emptyList(),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем, что фильтр "Избранные" можно нажать
        composeTestRule
            .onNodeWithText("Избранные")
            .assertHasClickAction()
            .performClick()
        
        // Проверяем, что фильтр "Следующий" можно нажать
        composeTestRule
            .onNodeWithText("Следующий")
            .assertHasClickAction()
            .performClick()
    }
    
    @Test
    fun scheduleList_displaysScheduleTimes() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules.take(2),
                    schedulesYarovoe = emptyList(),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем отображение времени отправления
        composeTestRule
            .onNodeWithText("08:30")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("10:00")
            .assertIsDisplayed()
    }
    
    @Test
    fun scheduleList_displaysBackButton() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules,
                    schedulesYarovoe = emptyList(),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем наличие кнопки "Назад"
        composeTestRule
            .onNodeWithContentDescription("Назад")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun scheduleList_displaysNotificationButton() {
        composeTestRule.setContent {
            lets_go_slavgorodTheme {
                ScheduleList(
                    route = testRoute,
                    schedulesSlavgorod = testSchedules,
                    schedulesYarovoe = emptyList(),
                    schedulesVokzal = emptyList(),
                    schedulesSovhoz = emptyList(),
                    nextUpcomingSlavgorodId = null,
                    nextUpcomingYarovoeId = null,
                    nextUpcomingVokzalId = null,
                    nextUpcomingSovhozId = null,
                    onBackClick = {},
                    onNotificationClick = {},
                    onScrollOffsetChange = {},
                    viewModel = viewModel
                )
            }
        }
        
        // Проверяем наличие кнопки уведомлений
        composeTestRule
            .onNodeWithContentDescription("Настройки уведомлений")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
}

