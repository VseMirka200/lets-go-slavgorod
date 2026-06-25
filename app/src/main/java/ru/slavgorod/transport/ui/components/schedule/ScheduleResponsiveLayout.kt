package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale
import ru.slavgorod.transport.ui.theme.scaleSpForFontScale

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

internal fun resolveScheduleResponsiveLayout(
    containerWidth: Dp,
    fontScale: Float = 1f
): ScheduleResponsiveLayoutSpec {
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
            sectionHorizontalPadding = lerpDp(8.dp, 16.dp, progress, fontScale),
            sectionVerticalSpacing = lerpDp(6.dp, 10.dp, progress, fontScale),
            sectionColumnSpacing = lerpDp(1.dp, 6.dp, progress, fontScale),
            twoColumnLayoutMinWidth = TWO_COLUMN_LAYOUT_MIN_WIDTH
        ),
        header = ScheduleHeaderLayoutSpec(
            headerHorizontalPadding = lerpDp(8.dp, 16.dp, progress, fontScale),
            headerVerticalPadding = lerpDp(6.dp, 12.dp, progress, fontScale),
            headerItemSpacing = lerpDp(4.dp, 8.dp, progress, fontScale),
            headerTitleFontSize = lerpSp(16.sp, 19.sp, progress, fontScale),
            headerSubtitleFontSize = lerpSp(11.sp, 14.sp, progress, fontScale)
        ),
        card = ScheduleCardLayoutSpec(
            cardHorizontalPadding = lerpDp(8.dp, 14.dp, progress, fontScale),
            cardVerticalPadding = lerpDp(8.dp, 12.dp, progress, fontScale),
            cardInnerSpacing = lerpDp(4.dp, 8.dp, progress, fontScale),
            departureTimeFontSize = lerpSp(19.sp, 24.sp, progress, fontScale),
            departureTimeColumnWidth = lerpDp(52.dp, 68.dp, progress, fontScale),
            supportingTextFontSize = lerpSp(13.sp, 16.sp, progress, fontScale),
            stackCardContent = containerWidth < 200.dp
        ),
        upcoming = ScheduleUpcomingLayoutSpec(
            upcomingHorizontalPadding = lerpDp(10.dp, 16.dp, progress, fontScale),
            upcomingHeaderFontSize = lerpSp(13.sp, 18.sp, progress, fontScale),
            upcomingPrimaryTextSize = lerpSp(16.sp, 20.sp, progress, fontScale),
            upcomingSupportingTextSize = lerpSp(9.sp, 13.sp, progress, fontScale),
            upcomingMinRowHeight = lerpDp(20.dp, 30.dp, progress, fontScale),
            upcomingCardContentPadding = lerpDp(8.dp, 12.dp, progress, fontScale),
            upcomingCardTopPadding = lerpDp(4.dp, 8.dp, progress, fontScale),
            upcomingCardBottomPadding = lerpDp(8.dp, 12.dp, progress, fontScale),
            upcomingBottomSpacing = lerpDp(4.dp, 8.dp, progress, fontScale)
        ),
        filters = ScheduleFiltersLayoutSpec(
            filterSheetHorizontalPadding = lerpDp(8.dp, 16.dp, progress, fontScale),
            filterSheetVerticalSpacing = lerpDp(6.dp, 8.dp, progress, fontScale)
        ),
        emptyState = ScheduleEmptyStateLayoutSpec(
            emptyStateHorizontalPadding = lerpDp(12.dp, 20.dp, progress, fontScale),
            emptyStateVerticalSpacing = lerpDp(8.dp, 12.dp, progress, fontScale)
        )
    )
}

private val MIN_LAYOUT_WIDTH = 160.dp
private val MAX_LAYOUT_WIDTH = 600.dp
private val TWO_COLUMN_LAYOUT_MIN_WIDTH = 520.dp

private fun lerpDp(start: Dp, stop: Dp, fraction: Float, fontScale: Float): Dp {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return scaleDpForFontScale(
        (start.value + (stop.value - start.value) * clampedFraction).dp,
        fontScale
    )
}

private fun lerpSp(start: TextUnit, stop: TextUnit, fraction: Float, fontScale: Float): TextUnit {
    val clampedFraction = fraction.coerceIn(0f, 1f)
    return scaleSpForFontScale(
        (start.value + (stop.value - start.value) * clampedFraction).sp,
        fontScale
    )
}
