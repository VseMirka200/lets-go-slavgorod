package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.Dp

internal fun shouldUseTwoColumnScheduleLayout(
    availableWidth: Dp,
    itemCount: Int,
    layout: ScheduleResponsiveLayoutSpec
): Boolean {
    return availableWidth >= layout.section.twoColumnLayoutMinWidth && itemCount >= 4
}
