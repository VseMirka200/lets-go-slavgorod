package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.Dp

/** Keep the user's column preference as an upper bound; reserve space for enlarged text. */
internal fun adaptiveColumnCount(
    availableWidth: Dp,
    fontScale: Float,
    preferredColumns: Int,
    minColumnWidth: Dp,
    spacing: Dp
): Int {
    val minimum = minColumnWidth.value * fontScale.coerceAtLeast(1f)
    val fitting = ((availableWidth.value + spacing.value) / (minimum + spacing.value)).toInt()
    return fitting.coerceIn(1, preferredColumns.coerceAtLeast(1))
}
