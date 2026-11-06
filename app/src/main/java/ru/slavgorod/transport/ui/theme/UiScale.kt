package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

internal fun scaleDpForFontScale(
    value: Dp,
    fontScale: Float,
    maxScale: Float = 1.35f
): Dp {
    val scale = resolvedScale(fontScale, maxScale)
    return (value.value * scale).dp
}

private fun resolvedScale(fontScale: Float, maxScale: Float): Float {
    return max(1f, min(fontScale, maxScale))
}
