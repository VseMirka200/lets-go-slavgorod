package com.example.lets_go_slavgorod.ui.navigation

/**
 * Экраны навигации приложения
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object FavoriteTimes : Screen("favorite_times")
    object Settings : Screen("settings")
    object About : Screen("about")
}
