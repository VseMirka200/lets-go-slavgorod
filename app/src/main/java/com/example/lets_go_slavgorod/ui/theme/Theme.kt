package com.example.lets_go_slavgorod.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Тема приложения "Поехали! Славгород"
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Определяет цветовые схемы приложения для светлой и темной темы
 * в соответствии с Material Design 3.
 * 
 * Цветовая палитра:
 * - **BusBlue** (#1976D2) - основной цвет автобусной тематики
 * - **AccentTeal** (#26A69A) - акцентный цвет для важных элементов
 * - **TransportOrange** (#FF9800) - для предупреждений и выделений
 * - **TransportRed** (#F44336) - для ошибок и критических уведомлений
 * 
 * Поддержка:
 * - Светлая тема (LightColorScheme)
 * - Темная тема (DarkColorScheme)
 * - Динамические цвета Android 12+ (опционально)
 * - Автоматическое переключение по системным настройкам
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */

private val DarkColorScheme = darkColorScheme(
    primary = BusBlueLight,
    onPrimary = Color.Black,
    primaryContainer = BusBlueDark,
    onPrimaryContainer = Color.White,
    
    secondary = AccentTeal,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00695C),
    onSecondaryContainer = Color.White,
    
    tertiary = TransportOrange,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE65100),
    onTertiaryContainer = Color.White,
    
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = Color(0xFF2C2C2C),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF3C3C3C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF424242),
    error = TransportRed,
    onError = Color.White,
    errorContainer = Color(0xFFD32F2F),
    onErrorContainer = Color.White,
    
    scrim = Color.Black.copy(alpha = 0.5f)
)

private val LightColorScheme = lightColorScheme(
    primary = BusBlue,
    onPrimary = Color.White,
    primaryContainer = BusBlueLight,
    onPrimaryContainer = BusBlueDark,
    
    secondary = AccentTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF80CBC4),
    onSecondaryContainer = Color(0xFF004D40),
    
    tertiary = TransportOrange,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFCC02),
    onTertiaryContainer = Color(0xFFE65100),
    
    background = Color(0xFFFFFBFE),
    onBackground = OnSurfaceLight,
    surface = Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = Color(0xFF424242),
    
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFFE0E0E0),
    error = TransportRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFFD32F2F),
    
    scrim = Color.Black.copy(alpha = 0.3f)
)

@Composable
fun lets_go_slavgorodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}