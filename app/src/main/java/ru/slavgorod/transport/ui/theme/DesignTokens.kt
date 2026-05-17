package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object DesignTokens {

    object Spacing {
        val Small: Dp = 8.dp
        val Medium: Dp = 16.dp
        val Large: Dp = 24.dp
    }

    object Size {
        object Card {
            object Height {
                val Grid1Column: Dp = 220.dp
                val Grid2Columns: Dp = 180.dp
                val Grid3Columns: Dp = 160.dp
                val Grid4Columns: Dp = 140.dp
            }
        }

        object Button {
            val Height: Dp = 44.dp
            val MinWidth: Dp = 88.dp
            val HorizontalPadding: Dp = 16.dp
            val VerticalPadding: Dp = 8.dp
            val IconButtonSize: Dp = 36.dp
        }
    }
}
