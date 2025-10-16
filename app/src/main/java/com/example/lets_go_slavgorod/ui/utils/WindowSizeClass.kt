package com.example.lets_go_slavgorod.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Утилиты для адаптивного дизайна
 * 
 * Предоставляет информацию о размере экрана для создания
 * адаптивных интерфейсов для телефонов, планшетов и складных устройств.
 * 
 * @author VseMirka200
 * @version 1.0
 */

/**
 * Класс размера окна
 */
enum class WindowSizeClass {
    /** Компактный (телефоны в портретной ориентации, < 600dp) */
    COMPACT,
    
    /** Средний (большие телефоны, маленькие планшеты, 600-840dp) */
    MEDIUM,
    
    /** Расширенный (планшеты, складные устройства, > 840dp) */
    EXPANDED
}

/**
 * Информация о размере окна
 */
data class WindowSize(
    val width: Dp,
    val height: Dp,
    val widthSizeClass: WindowSizeClass,
    val heightSizeClass: WindowSizeClass
)

/**
 * Composable функция для получения информации о размере окна
 */
@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val width = configuration.screenWidthDp.dp
        val height = configuration.screenHeightDp.dp
        
        WindowSize(
            width = width,
            height = height,
            widthSizeClass = when {
                width < 600.dp -> WindowSizeClass.COMPACT
                width < 840.dp -> WindowSizeClass.MEDIUM
                else -> WindowSizeClass.EXPANDED
            },
            heightSizeClass = when {
                height < 480.dp -> WindowSizeClass.COMPACT
                height < 900.dp -> WindowSizeClass.MEDIUM
                else -> WindowSizeClass.EXPANDED
            }
        )
    }
}

/**
 * Адаптивное количество колонок для сетки
 */
@Composable
fun rememberAdaptiveGridColumns(
    compact: Int = 2,
    medium: Int = 3,
    expanded: Int = 4
): Int {
    val windowSize = rememberWindowSize()
    return when (windowSize.widthSizeClass) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Адаптивные отступы
 */
@Composable
fun rememberAdaptivePadding(
    compact: Dp = 16.dp,
    medium: Dp = 24.dp,
    expanded: Dp = 32.dp
): Dp {
    val windowSize = rememberWindowSize()
    return when (windowSize.widthSizeClass) {
        WindowSizeClass.COMPACT -> compact
        WindowSizeClass.MEDIUM -> medium
        WindowSizeClass.EXPANDED -> expanded
    }
}

/**
 * Проверка на планшет
 */
@Composable
fun isTablet(): Boolean {
    val windowSize = rememberWindowSize()
    return windowSize.widthSizeClass >= WindowSizeClass.MEDIUM
}

/**
 * Проверка на складное устройство
 */
@Composable
fun isFoldable(): Boolean {
    val windowSize = rememberWindowSize()
    return windowSize.widthSizeClass == WindowSizeClass.EXPANDED
}

