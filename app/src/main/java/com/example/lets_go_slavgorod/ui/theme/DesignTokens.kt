package com.example.lets_go_slavgorod.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design Tokens - система дизайна приложения
 * 
 * Централизованное хранилище всех дизайн-токенов для обеспечения
 * консистентности UI и упрощения глобальных изменений дизайна.
 * 
 * @author VseMirka200
 * @version 1.0
 */
object DesignTokens {
    
    // =====================================================================================
    //                              SPACING (Отступы)
    // =====================================================================================
    
    object Spacing {
        val None: Dp = 0.dp
        val ExtraSmall: Dp = 4.dp
        val Small: Dp = 8.dp
        val Medium: Dp = 16.dp
        val Large: Dp = 24.dp
        val ExtraLarge: Dp = 32.dp
        val Huge: Dp = 48.dp
    }
    
    // =====================================================================================
    //                              CORNER RADIUS (Скругления)
    // =====================================================================================
    
    object CornerRadius {
        val None: Dp = 0.dp
        val Small: Dp = 4.dp
        val Medium: Dp = 8.dp
        val Large: Dp = 12.dp
        val ExtraLarge: Dp = 16.dp
        val Round: Dp = 24.dp
    }
    
    // =====================================================================================
    //                              ELEVATION (Тени)
    // =====================================================================================
    
    object Elevation {
        val None: Dp = 0.dp
        val Level1: Dp = 1.dp
        val Level2: Dp = 2.dp
        val Level3: Dp = 4.dp
        val Level4: Dp = 6.dp
        val Level5: Dp = 8.dp
    }
    
    // =====================================================================================
    //                              SIZE (Размеры)
    // =====================================================================================
    
    object Size {
        object Icon {
            val Small: Dp = 16.dp
            val Medium: Dp = 24.dp
            val Large: Dp = 32.dp
            val ExtraLarge: Dp = 48.dp
            val Huge: Dp = 64.dp
        }
        
        object Button {
            val Height: Dp = 48.dp
            val HeightSmall: Dp = 36.dp
            val HeightLarge: Dp = 56.dp
        }
        
        object Card {
            object Height {
                val Grid1Column: Dp = 220.dp
                val Grid2Columns: Dp = 180.dp
                val Grid3Columns: Dp = 160.dp
                val Grid4Columns: Dp = 140.dp
                val List: Dp = 120.dp
            }
        }
    }
    
    // =====================================================================================
    //                              ANIMATION (Анимации)
    // =====================================================================================
    
    object Animation {
        const val DurationFast = 150
        const val DurationNormal = 300
        const val DurationSlow = 500
        const val DurationVerySlow = 800
    }
    
    // =====================================================================================
    //                              OPACITY (Прозрачность)
    // =====================================================================================
    
    object Opacity {
        const val Disabled = 0.38f
        const val Medium = 0.6f
        const val High = 0.87f
        const val Full = 1.0f
    }
    
    // =====================================================================================
    //                              Z-INDEX (Слои)
    // =====================================================================================
    
    object ZIndex {
        const val Background = 0f
        const val Content = 1f
        const val Overlay = 2f
        const val Dialog = 3f
        const val Snackbar = 4f
        const val Tooltip = 5f
    }
}

