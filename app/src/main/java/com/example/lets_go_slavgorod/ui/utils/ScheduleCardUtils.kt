package com.example.lets_go_slavgorod.ui.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.example.lets_go_slavgorod.utils.Constants

/**
 * Утилиты для карточек расписания
 * 
 * Содержит вспомогательные функции для унификации стиля и поведения
 * карточек расписания в приложении. Помогает избежать дублирования кода
 * и обеспечивает единообразие UI.
 * 
 * Функции:
 * - Получение цветов для различных состояний карточек
 * - Расчет размеров elevation
 * - Вспомогательные функции для форматирования
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 3.0
 */
object ScheduleCardUtils {
    
    /**
     * Получает цвет контейнера карточки в зависимости от состояния
     * 
     * @param isNextUpcoming является ли карточка ближайшим рейсом
     * @param colorScheme цветовая схема Material Design
     * @return цвет для фона карточки
     */
    fun getContainerColor(isNextUpcoming: Boolean, colorScheme: ColorScheme): Color {
        return if (isNextUpcoming) {
            colorScheme.primaryContainer
        } else {
            colorScheme.surfaceVariant
        }
    }
    
    /**
     * Получает цвет текста карточки в зависимости от состояния
     * 
     * @param isNextUpcoming является ли карточка ближайшим рейсом
     * @param colorScheme цветовая схема Material Design
     * @return цвет для текста
     */
    fun getTextColor(isNextUpcoming: Boolean, colorScheme: ColorScheme): Color {
        return if (isNextUpcoming) {
            colorScheme.primary
        } else {
            colorScheme.onSurface
        }
    }
    
    /**
     * Получает цвет иконки избранного
     * 
     * @param isFavorite добавлено ли в избранное
     * @param colorScheme цветовая схема Material Design
     * @return цвет для иконки
     */
    fun getFavoriteIconColor(isFavorite: Boolean, colorScheme: ColorScheme): Color {
        return if (isFavorite) {
            colorScheme.primary
        } else {
            colorScheme.onSurfaceVariant
        }
    }
    
    /**
     * Получает текст для accessibility описания иконки избранного
     * 
     * @param isFavorite добавлено ли в избранное
     * @return текст описания
     */
    fun getFavoriteContentDescription(isFavorite: Boolean): String {
        return if (isFavorite) {
            "Убрать из избранного"
        } else {
            "Добавить в избранное"
        }
    }
    
    /**
     * Получает размер elevation карточки в dp
     * 
     * @param isNextUpcoming является ли карточка ближайшим рейсом
     * @return значение elevation в dp
     */
    fun getElevation(isNextUpcoming: Boolean): Int {
        return if (isNextUpcoming) {
            Constants.SCHEDULE_CARD_ELEVATION_UPCOMING
        } else {
            Constants.SCHEDULE_CARD_ELEVATION_DEFAULT
        }
    }
}

