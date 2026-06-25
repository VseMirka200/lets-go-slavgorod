package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

internal fun scaleSpForFontScale(
    value: TextUnit,
    fontScale: Float,
    maxScale: Float = 1.35f
): TextUnit {
    val scale = resolvedScale(fontScale, maxScale)
    return (value.value * scale).sp
}

private fun resolvedScale(fontScale: Float, maxScale: Float): Float {
    return max(1f, min(fontScale, maxScale))
}
