package com.example.lets_go_slavgorod.ui.navigation

import timber.log.Timber
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Нижняя панель навигации для основных экранов приложения
 * 
 * Отображает основные разделы приложения:
 * - Главная: список маршрутов
 * - Избранное: сохраненные маршруты  
 * - Настройки: конфигурация приложения
 * - О программе: информация и поддержка
 * 
 * @param navController контроллер навигации для управления переходами
 */
@Composable
fun BottomNavigation(navController: NavController) {
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentScreenRoute = currentRoute?.destination?.route

    NavigationBar {
        bottomNavItems.forEach { screen ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = { Text(screen.title) },
                selected = currentScreenRoute == screen.route,
                onClick = {
                    try {
                        Timber.d("Navigating to: ${screen.route}")
                        navController.navigate(screen.route) {
                            // Специальная обработка для настроек и избранных
                            when (screen.route) {
                                Screen.Settings.route, Screen.FavoriteTimes.route -> {
                                    // Очищаем весь стек и открываем экран заново
                                    popUpTo(0) { inclusive = false }
                                }
                                else -> {
                                    // Очищаем стек навигации при переходе к основным экранам
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                }
                            }
                            launchSingleTop = true
                            restoreState = when (screen.route) {
                                Screen.FavoriteTimes.route -> false  // Не восстанавливаем состояние для избранных
                                else -> true
                            }
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Navigation error to: ${screen.route}")
                    }
                }
            )
        }
    }
}