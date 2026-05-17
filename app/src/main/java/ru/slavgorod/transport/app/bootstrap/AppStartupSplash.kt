package ru.slavgorod.transport.app.bootstrap

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.viewmodel.RoutesUiState

internal const val STARTUP_SPLASH_MIN_DURATION_MS = 1_600L
internal const val STARTUP_SPLASH_MAX_DURATION_MS = 4_000L
private const val STARTUP_LOGO_TRAVEL_DURATION_MS = 1_800
private val STARTUP_LOGO_SIZE = 120.dp
private const val STARTUP_LOGO_TRAVEL_DISTANCE_PX = 220f

internal fun shouldShowStartupSplash(
    routesUiState: RoutesUiState,
    isSplashMinDurationReached: Boolean,
    isSplashMaxDurationReached: Boolean
): Boolean {
    val isInitialDataReady =
        routesUiState.routes.isNotEmpty() ||
                (!routesUiState.isLoading) ||
                (routesUiState.error != null)
    val shouldHideSplash =
        isSplashMaxDurationReached || (isSplashMinDurationReached && isInitialDataReady)
    return !shouldHideSplash
}

@Composable
internal fun rememberShouldShowStartupSplash(routesUiState: RoutesUiState): Boolean {
    var isSplashMinDurationReached by remember { mutableStateOf(false) }
    var isSplashMaxDurationReached by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(STARTUP_SPLASH_MIN_DURATION_MS)
        isSplashMinDurationReached = true
    }

    LaunchedEffect(Unit) {
        delay(STARTUP_SPLASH_MAX_DURATION_MS)
        isSplashMaxDurationReached = true
    }

    return remember(
        routesUiState.routes,
        routesUiState.isLoading,
        routesUiState.error,
        isSplashMinDurationReached,
        isSplashMaxDurationReached
    ) {
        shouldShowStartupSplash(
            routesUiState = routesUiState,
            isSplashMinDurationReached = isSplashMinDurationReached,
            isSplashMaxDurationReached = isSplashMaxDurationReached
        )
    }
}

@Composable
internal fun StartupSplashOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "startup_logo_motion")
    val translationX by infiniteTransition.animateFloat(
        initialValue = -STARTUP_LOGO_TRAVEL_DISTANCE_PX,
        targetValue = STARTUP_LOGO_TRAVEL_DISTANCE_PX,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STARTUP_LOGO_TRAVEL_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "startup_logo_translation_x"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_lets_go_slavgorod),
            contentDescription = null,
            modifier = Modifier
                .size(STARTUP_LOGO_SIZE)
                .graphicsLayer {
                    this.translationX = translationX
                }
        )
    }
}
