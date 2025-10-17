package com.example.lets_go_slavgorod.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Типографика приложения "Поехали! Славгород"
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Определяет все текстовые стили приложения в соответствии с Material Design 3.
 * Использует системный шрифт Roboto (FontFamily.Default) для всех элементов.
 * 
 * Категории стилей:
 * 
 * **Display** (57sp, 45sp, 36sp) - для очень крупных заголовков:
 * - Bold weight для максимального визуального воздействия
 * - Отрицательный letterSpacing для displayLarge
 * 
 * **Headline** (32sp, 28sp, 24sp) - для заголовков секций:
 * - SemiBold weight для баланса между читаемостью и акцентом
 * - Используется в заголовках экранов и карточках
 * 
 * **Title** (22sp, 16sp, 14sp) - для подзаголовков:
 * - Medium/SemiBold weight
 * - Используется в топбарах, карточках, диалогах
 * 
 * **Body** (16sp, 14sp, 12sp) - для основного текста:
 * - Normal weight для лучшей читаемости
 * - Увеличенный letterSpacing для улучшения читаемости
 * 
 * **Label** (14sp, 12sp, 11sp) - для меток и кнопок:
 * - Medium/SemiBold weight для контраста
 * - Используется в кнопках, чипах, подсказках
 * 
 * Изменения v2.0:
 * - Все стили явно используют FontFamily.Default (Roboto)
 * - Унифицированы размеры для всех заголовков
 * - Оптимизированы letterSpacing значения
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
val Typography = Typography(
    // Заголовки - более жирные и читаемые
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Основной текст - улучшенная читаемость
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    // Метки - более контрастные
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)