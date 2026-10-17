package ru.slavgorod.transport.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Blue300 = Color(0xFF42A5F5)
private val Blue700 = Color(0xFF1976D2)
private val Blue900 = Color(0xFF0D47A1)
private val Cyan500 = Color(0xFF00BCD4)
private val Teal200 = Color(0xFF80CBC4)
private val Teal800 = Color(0xFF00695C)
private val Teal900 = Color(0xFF004D40)
private val Orange500 = Color(0xFFFF9800)
private val Orange900 = Color(0xFFE65100)
private val Amber400 = Color(0xFFFFCC02)
private val Neutral100 = Color(0xFFE6E1E5)
private val Neutral200 = Color(0xFFE0E0E0)
private val Neutral300 = Color(0xFFBDBDBD)
private val Neutral500 = Color(0xFF757575)
private val Neutral700 = Color(0xFF424242)
private val Neutral800 = Color(0xFF3C3C3C)
private val Neutral900 = Color(0xFF2C2C2C)
private val Neutral950 = Color(0xFF1C1B1F)
private val Red100 = Color(0xFFFFDAD6)
private val Red600 = Color(0xFFE53935)
private val Red700 = Color(0xFFD32F2F)
private val WarmWhite = Color(0xFFFFFBFE)
private val BlackScrim30 = Color.Black.copy(alpha = 0.3f)
private val BlackScrim50 = Color.Black.copy(alpha = 0.5f)

private val DarkColorScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = Color.Black,
    primaryContainer = Blue900,
    onPrimaryContainer = Color.White,
    secondary = Cyan500,
    onSecondary = Color.Black,
    secondaryContainer = Teal800,
    onSecondaryContainer = Color.White,
    tertiary = Orange500,
    onTertiary = Color.Black,
    tertiaryContainer = Orange900,
    onTertiaryContainer = Color.White,
    background = Neutral950,
    onBackground = Neutral100,
    surface = Neutral900,
    onSurface = Color.White,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral300,
    outline = Neutral500,
    outlineVariant = Neutral700,
    error = Red600,
    onError = Color.White,
    errorContainer = Red700,
    onErrorContainer = Color.White,
    scrim = BlackScrim50
)

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue300,
    onPrimaryContainer = Blue900,
    secondary = Cyan500,
    onSecondary = Color.White,
    secondaryContainer = Teal200,
    onSecondaryContainer = Teal900,
    tertiary = Orange500,
    onTertiary = Color.White,
    tertiaryContainer = Amber400,
    onTertiaryContainer = Orange900,
    background = WarmWhite,
    onBackground = Neutral950,
    surface = Color.White,
    onSurface = Neutral950,
    surfaceVariant = Color.White,
    onSurfaceVariant = Neutral700,
    outline = Neutral500,
    outlineVariant = Neutral200,
    error = Red600,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red700,
    scrim = BlackScrim30
)

@Composable
fun LetsGoSlavgorodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = resolveColorScheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        context = context
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        content()
    }
}

private fun resolveColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    context: Context
): ColorScheme {
    return when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
}
