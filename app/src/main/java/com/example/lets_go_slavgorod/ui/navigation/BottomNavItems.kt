package com.example.lets_go_slavgorod.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings

/**
 * Данные для элемента нижней навигации
 * 
 * @param route маршрут экрана для навигации
 * @param icon иконка для отображения в нижней панели
 * @param title заголовок элемента навигации
 */
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String
)

/**
 * Список элементов нижней навигации приложения
 * 
 * Содержит основные экраны:
 * - Маршруты: основной экран с маршрутами
 * - Избранное: сохраненные пользователем маршруты
 * - Настройки: конфигурация приложения (включая раздел "О программе")
 */
val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Home.route,
        icon = Icons.Default.Home,
        title = "Маршруты"
    ),
    BottomNavItem(
        route = Screen.FavoriteTimes.route,
        icon = Icons.Default.Favorite,
        title = "Избранное"
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        icon = Icons.Default.Settings,
        title = "Настройки"
    )
)
