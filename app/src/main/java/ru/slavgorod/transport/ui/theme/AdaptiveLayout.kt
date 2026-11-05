package ru.slavgorod.transport.ui.theme

import androidx.compose.ui.unit.Dp

/** Ограничивает число столбцов настройкой пользователя и учитывает увеличение шрифта. */
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
