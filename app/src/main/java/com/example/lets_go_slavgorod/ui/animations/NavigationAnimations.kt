package com.example.lets_go_slavgorod.ui.animations

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

/**
 * Коллекция анимаций для навигации между экранами
 * 
 * Содержит предопределенные анимации переходов, оптимизированные
 * для различных типов экранов и пользовательских сценариев.
 * Все анимации используют Material Design принципы и обеспечивают
 * плавные переходы между экранами приложения.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
object NavigationAnimations {
    
    /**
     * Классический слайд-эффект как в мобильных телефонах
     * Новый экран входит справа, текущий уходит влево
     */
    val slideInFromRight = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
    
    val slideOutToLeft = slideOutHorizontally(
        targetOffsetX = { fullWidth -> -fullWidth },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )

    /**
     * Анимация для экрана расписания
     * Использует вертикальное движение с fade-эффектом для более мягкого перехода
     */
    val slideInFromBottomSchedule = slideInVertically(
        initialOffsetY = { fullHeight -> fullHeight / 2 },
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    ) + fadeIn(
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        )
    )
    
    val slideOutToBottomSchedule = slideOutVertically(
        targetOffsetY = { fullHeight -> fullHeight / 2 },
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    ) + fadeOut(
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        )
    )
}
