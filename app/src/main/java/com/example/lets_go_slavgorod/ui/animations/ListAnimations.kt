package com.example.lets_go_slavgorod.ui.animations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Анимации для списков и элементов UI
 * 
 * Предоставляет современные анимации появления и исчезновения элементов,
 * улучшая визуальную обратную связь для пользователя.
 * 
 * @author VseMirka200
 * @version 1.0
 */

/**
 * Анимация появления элемента списка с задержкой
 */
@Composable
fun AnimatedListItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 50L).coerceAtMost(300L))
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            initialOffsetY = { it / 4 }
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            ),
            initialScale = 0.9f
        ),
        exit = fadeOut() + slideOutVertically() + scaleOut()
    ) {
        content()
    }
}

/**
 * Модификатор для пульсирующей анимации (для избранного)
 */
@Composable
fun Modifier.pulseAnimation(enabled: Boolean = true): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (enabled) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    return if (enabled) {
        this.scale(scale)
    } else {
        this
    }
}

/**
 * Модификатор для анимации тряски (при ошибке)
 */
@Composable
fun Modifier.shakeAnimation(trigger: Boolean): Modifier {
    var currentTrigger by remember { mutableStateOf(trigger) }
    val shake by animateFloatAsState(
        targetValue = if (currentTrigger) 0f else 1f,
        animationSpec = tween(
            durationMillis = 500,
            easing = LinearEasing
        ),
        label = "shake"
    )
    
    LaunchedEffect(trigger) {
        if (trigger != currentTrigger) {
            currentTrigger = trigger
        }
    }
    
    return this.graphicsLayer {
        translationX = if (currentTrigger) {
            (shake * 20 * kotlin.math.sin(shake * kotlin.math.PI * 4)).toFloat()
        } else {
            0f
        }
    }
}

/**
 * Анимация подсветки нового элемента
 */
@Composable
fun Modifier.highlightNewItem(isNew: Boolean): Modifier {
    val alpha by animateFloatAsState(
        targetValue = if (isNew) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "highlight"
    )
    
    return this.graphicsLayer {
        this.alpha = alpha
    }
}

