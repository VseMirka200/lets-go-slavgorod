package ru.slavgorod.transport.ui.components.schedule

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.ui.components.CompactScheduleCard

internal fun filterSchedules(
    schedules: List<BusSchedule>,
    showOnlyUpcoming: Boolean,
    nextUpcomingId: String?
): List<BusSchedule> {
    return when {
        showOnlyUpcoming -> schedules.filter { schedule -> schedule.id == nextUpcomingId }
        else -> schedules
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun SingleColumnScheduleList(
    title: String,
    schedules: List<BusSchedule>,
    nextUpcomingId: String?,
    layout: ScheduleResponsiveLayoutSpec,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    showOnlyUpcoming: Boolean,
    showDayLabel: Boolean = false,
    extraLabel: String? = null,
    compactSpacing: Boolean = false,
    extraLabelProvider: ((BusSchedule) -> String?)? = null,
    modifier: Modifier = Modifier
) {
    val horizontalPadding = layout.section.sectionHorizontalPadding
    val verticalPadding = layout.section.sectionVerticalSpacing
    val titlePadding = if (layout.mode == ScheduleResponsiveLayoutMode.TIGHT) 2.dp else 4.dp
    val collapsedTitleHorizontalPadding = if (isCollapsed) 0.dp else titlePadding
    val collapsedTitleVerticalPadding =
        if (isCollapsed) layout.section.sectionVerticalSpacing else 4.dp
    val itemSpacing = 0.dp

    val filteredSchedules = filterSchedules(
        schedules = schedules,
        showOnlyUpcoming = showOnlyUpcoming,
        nextUpcomingId = nextUpcomingId
    )

    if (filteredSchedules.isEmpty()) {
        if (showOnlyUpcoming) {
            EmptySingleColumnScheduleState(layout = layout, modifier = modifier)
        }
        return
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = verticalPadding),
            verticalArrangement = Arrangement.spacedBy(if (compactSpacing) 4.dp else itemSpacing)
        ) {
            if (title.isNotEmpty()) {
                val arrowRotation = if (isCollapsed) 180f else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            enabled = onToggleCollapse != null,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onToggleCollapse?.invoke()
                        }
                        .padding(horizontal = horizontalPadding)
                        .padding(
                            horizontal = collapsedTitleHorizontalPadding,
                            vertical = collapsedTitleVerticalPadding
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = layout.header.headerTitleFontSize,
                                lineHeight = layout.header.headerTitleFontSize * 1.1f
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = Int.MAX_VALUE,
                            softWrap = true,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (onToggleCollapse != null) {
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.graphicsLayer {
                                    rotationZ = arrowRotation
                                }
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useTwoColumns = shouldUseTwoColumnScheduleLayout(
                        availableWidth = maxWidth,
                        itemCount = filteredSchedules.size,
                        layout = layout
                    )
                    val columnSpacing = layout.section.sectionColumnSpacing

                    if (useTwoColumns) {
                        val splitIndex = (filteredSchedules.size + 1) / 2
                        val leftColumnSchedules = filteredSchedules.take(splitIndex)
                        val rightColumnSchedules = filteredSchedules.drop(splitIndex)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = horizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(columnSpacing),
                            verticalAlignment = Alignment.Top
                        ) {
                            ScheduleCardColumn(
                                schedules = leftColumnSchedules,
                                nextUpcomingId = nextUpcomingId,
                                layout = layout,
                                showDayLabel = showDayLabel,
                                extraLabel = extraLabel,
                                extraLabelProvider = extraLabelProvider,
                                itemSpacing = itemSpacing,
                                modifier = Modifier.weight(1f)
                            )

                            ScheduleCardColumn(
                                schedules = rightColumnSchedules,
                                nextUpcomingId = nextUpcomingId,
                                layout = layout,
                                showDayLabel = showDayLabel,
                                extraLabel = extraLabel,
                                extraLabelProvider = extraLabelProvider,
                                itemSpacing = itemSpacing,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        ScheduleCardColumn(
                            schedules = filteredSchedules,
                            nextUpcomingId = nextUpcomingId,
                            layout = layout,
                            showDayLabel = showDayLabel,
                            extraLabel = extraLabel,
                            extraLabelProvider = extraLabelProvider,
                            itemSpacing = itemSpacing,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleCardColumn(
    schedules: List<BusSchedule>,
    nextUpcomingId: String?,
    layout: ScheduleResponsiveLayoutSpec,
    showDayLabel: Boolean,
    extraLabel: String?,
    extraLabelProvider: ((BusSchedule) -> String?)?,
    itemSpacing: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(itemSpacing)
    ) {
        schedules.forEachIndexed { index, schedule ->
            CompactScheduleCard(
                schedule = schedule,
                layout = layout,
                isNextUpcoming = schedule.id == nextUpcomingId,
                showDayLabel = showDayLabel,
                extraLabel = extraLabelProvider?.invoke(schedule) ?: extraLabel,
                showBottomDivider = index != schedules.lastIndex,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EmptySingleColumnScheduleState(
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.schedule_no_upcoming_departures),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = layout.emptyState.emptyStateHorizontalPadding)
        )
    }
}
