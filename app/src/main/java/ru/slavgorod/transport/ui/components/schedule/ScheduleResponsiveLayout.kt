package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class ScheduleResponsiveLayoutMode {
    TIGHT,
    COMPACT,
    REGULAR
}

internal data class ScheduleResponsiveLayoutSpec(
    val mode: ScheduleResponsiveLayoutMode,
    val section: ScheduleSectionLayoutSpec,
    val header: ScheduleHeaderLayoutSpec,
    val card: ScheduleCardLayoutSpec,
    val upcoming: ScheduleUpcomingLayoutSpec,
    val filters: ScheduleFiltersLayoutSpec,
    val emptyState: ScheduleEmptyStateLayoutSpec
)

internal data class ScheduleSectionLayoutSpec(
    val sectionHorizontalPadding: Dp,
    val sectionVerticalSpacing: Dp,
    val sectionColumnSpacing: Dp,
    val twoColumnLayoutMinWidth: Dp
)

internal data class ScheduleHeaderLayoutSpec(
    val headerHorizontalPadding: Dp,
    val headerVerticalPadding: Dp,
    val headerItemSpacing: Dp,
    val headerTitleFontSize: TextUnit,
    val headerSubtitleFontSize: TextUnit
)

internal data class ScheduleCardLayoutSpec(
    val cardHorizontalPadding: Dp,
    val cardVerticalPadding: Dp,
    val cardInnerSpacing: Dp,
    val departureTimeFontSize: TextUnit,
    val departureTimeColumnWidth: Dp,
    val supportingTextFontSize: TextUnit,
    val stackCardContent: Boolean
)

internal data class ScheduleUpcomingLayoutSpec(
    val upcomingHorizontalPadding: Dp,
    val upcomingHeaderFontSize: TextUnit,
    val upcomingPrimaryTextSize: TextUnit,
    val upcomingSupportingTextSize: TextUnit,
    val upcomingMinRowHeight: Dp,
    val upcomingCardContentPadding: Dp,
    val upcomingCardTopPadding: Dp,
    val upcomingCardBottomPadding: Dp,
    val upcomingBottomSpacing: Dp
)

internal data class ScheduleFiltersLayoutSpec(
    val filterSheetHorizontalPadding: Dp,
    val filterSheetVerticalSpacing: Dp
)

internal data class ScheduleEmptyStateLayoutSpec(
    val emptyStateHorizontalPadding: Dp,
    val emptyStateVerticalSpacing: Dp
)

internal fun resolveScheduleResponsiveLayout(containerWidth: Dp): ScheduleResponsiveLayoutSpec {
    val clampedWidth =
        containerWidth.value.coerceIn(MIN_LAYOUT_WIDTH.value, MAX_LAYOUT_WIDTH.value).dp
    val progress =
        ((clampedWidth.value - MIN_LAYOUT_WIDTH.value) / (MAX_LAYOUT_WIDTH.value - MIN_LAYOUT_WIDTH.value))
            .coerceIn(0f, 1f)

    return ScheduleResponsiveLayoutSpec(
        mode = when {
            containerWidth < 200.dp -> ScheduleResponsiveLayoutMode.TIGHT
            containerWidth < 420.dp -> ScheduleResponsiveLayoutMode.COMPACT
            else -> ScheduleResponsiveLayoutMode.REGULAR
        },
        section = ScheduleSectionLayoutSpec(
            sectionHorizontalPadding = lerpDp(8.dp, 16.dp, progress),
            sectionVerticalSpacing = lerpDp(6.dp, 10.dp, progress),
            sectionColumnSpacing = lerpDp(1.dp, 6.dp, progress),
            twoColumnLayoutMinWidth = TWO_COLUMN_LAYOUT_MIN_WIDTH
        ),
        header = ScheduleHeaderLayoutSpec(
            headerHorizontalPadding = lerpDp(8.dp, 16.dp, progress),
            headerVerticalPadding = lerpDp(6.dp, 12.dp, progress),
            headerItemSpacing = lerpDp(4.dp, 8.dp, progress),
            headerTitleFontSize = lerpSp(16.sp, 19.sp, progress),
            headerSubtitleFontSize = lerpSp(11.sp, 14.sp, progress)
        ),
        card = ScheduleCardLayoutSpec(
            cardHorizontalPadding = lerpDp(8.dp, 14.dp, progress),
            cardVerticalPadding = lerpDp(8.dp, 12.dp, progress),
            cardInnerSpacing = lerpDp(4.dp, 8.dp, progress),
            departureTimeFontSize = lerpSp(19.sp, 24.sp, progress),
            departureTimeColumnWidth = lerpDp(52.dp, 68.dp, progress),
            supportingTextFontSize = lerpSp(13.sp, 16.sp, progress),
            stackCardContent = containerWidth < 200.dp
        ),
        upcoming = ScheduleUpcomingLayoutSpec(
            upcomingHorizontalPadding = lerpDp(10.dp, 16.dp, progress),
            upcomingHeaderFontSize = lerpSp(13.sp, 18.sp, progress),
            upcomingPrimaryTextSize = lerpSp(16.sp, 20.sp, progress),
            upcomingSupportingTextSize = lerpSp(9.sp, 13.sp, progress),
            upcomingMinRowHeight = lerpDp(20.dp, 30.dp, progress),
            upcomingCardContentPadding = lerpDp(8.dp, 12.dp, progress),
            upcomingCardTopPadding = lerpDp(4.dp, 8.dp, progress),
            upcomingCardBottomPadding = lerpDp(8.dp, 12.dp, progress),
            upcomingBottomSpacing = lerpDp(4.dp, 8.dp, progress)
        ),
        filters = ScheduleFiltersLayoutSpec(
            filterSheetHorizontalPadding = lerpDp(8.dp, 16.dp, progress),
            filterSheetVerticalSpacing = lerpDp(6.dp, 8.dp, progress)
        ),
        emptyState = ScheduleEmptyStateLayoutSpec(
            emptyStateHorizontalPadding = lerpDp(12.dp, 20.dp, progress),
            emptyStateVerticalSpacing = lerpDp(8.dp, 12.dp, progress)
        )
    )
}

private val MIN_LAYOUT_WIDTH = 160.dp
private val MAX_LAYOUT_WIDTH = 600.dp
private val TWO_COLUMN_LAYOUT_MIN_WIDTH = 520.dp

private fun lerpDp(start: Dp, stop: Dp, fraction: Float): Dp {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return (start.value + (stop.value - start.value) * clampedFraction).dp
}

private fun lerpSp(start: TextUnit, stop: TextUnit, fraction: Float): TextUnit {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return (start.value + (stop.value - start.value) * clampedFraction).sp
}
