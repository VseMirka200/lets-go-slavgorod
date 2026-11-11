package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.logging.UserActionLogger
import kotlin.math.roundToInt

internal data class ScheduleSectionDescriptor(
    val key: String,
    val title: String,
    val schedules: List<BusSchedule>,
    val nextUpcomingId: String?
)

internal enum class SectionSwapMode {
    SWAP_FIRST_PAIR,
    REVERSE_SECTION_ORDER
}

internal fun LazyListScope.addSingleColumnScheduleItem(
    itemKey: String,
    schedules: List<BusSchedule>,
    title: String,
    nextUpcomingId: String?,
    showOnlyUpcoming: Boolean,
    layout: ScheduleResponsiveLayoutSpec,
    isCollapsed: Boolean = false,
    onToggleCollapse: (() -> Unit)? = null,
    compactSpacing: Boolean = false,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)? = null,
    horizontalPadding: Dp = 12.dp,
    bottomPadding: Dp = 8.dp
) {
    item(key = itemKey) {
        SingleColumnScheduleList(
            title = title,
            schedules = schedules,
            nextUpcomingId = nextUpcomingId,
            layout = layout,
            isCollapsed = isCollapsed,
            onToggleCollapse = onToggleCollapse,
            showOnlyUpcoming = showOnlyUpcoming,
            compactSpacing = compactSpacing,
            extraLabelProvider = scheduleExtraLabelProvider,
            modifier = Modifier.padding(
                start = horizontalPadding,
                end = horizontalPadding,
                bottom = bottomPadding
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.addSectionSequenceLayout(
    sections: List<ScheduleSectionDescriptor>,
    keyPrefix: String,
    showOnlyUpcoming: Boolean,
    swapScheduleColumns: Boolean = false,
    collapsedStates: MutableMap<String, Boolean> = mutableMapOf(),
    swapMode: SectionSwapMode = SectionSwapMode.SWAP_FIRST_PAIR,
    compactSpacing: Boolean = false,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)? = null,
    layout: ScheduleResponsiveLayoutSpec,
    horizontalPadding: Dp = 12.dp,
    bottomPadding: Dp = 8.dp
) {
    val orderedSections = sections
        .filter { it.schedules.isNotEmpty() }
        .let { nonEmptySections ->
            if (swapMode == SectionSwapMode.REVERSE_SECTION_ORDER && swapScheduleColumns) {
                nonEmptySections.reversed()
            } else {
                nonEmptySections
            }
        }

    when (orderedSections.size) {
        0 -> Unit
        1 -> {
            val singleSection = orderedSections.first()
            addSectionItem(
                section = singleSection,
                itemKey = sectionItemKey(keyPrefix, singleSection),
                showOnlyUpcoming = showOnlyUpcoming,
                collapsedStates = collapsedStates,
                layout = layout,
                compactSpacing = compactSpacing,
                scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                horizontalPadding = horizontalPadding,
                bottomPadding = bottomPadding
            )
        }

        else -> {
            orderedSections.forEach { section ->
                addSectionItem(
                    section = section,
                    itemKey = sectionItemKey(keyPrefix, section),
                    showOnlyUpcoming = showOnlyUpcoming,
                    collapsedStates = collapsedStates,
                    layout = layout,
                    compactSpacing = compactSpacing,
                    scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                    horizontalPadding = horizontalPadding,
                    bottomPadding = bottomPadding
                )
            }
        }
    }
}

private fun sectionItemKey(keyPrefix: String, section: ScheduleSectionDescriptor): String {
    return "${keyPrefix}_single_${section.key}"
}

private fun LazyListScope.addSectionItem(
    section: ScheduleSectionDescriptor,
    itemKey: String,
    showOnlyUpcoming: Boolean,
    collapsedStates: MutableMap<String, Boolean>,
    layout: ScheduleResponsiveLayoutSpec,
    compactSpacing: Boolean,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?,
    horizontalPadding: Dp,
    bottomPadding: Dp
) {
    addSingleColumnScheduleItem(
        itemKey = itemKey,
        schedules = section.schedules,
        title = section.title,
        nextUpcomingId = section.nextUpcomingId,
        showOnlyUpcoming = showOnlyUpcoming,
        layout = layout,
        compactSpacing = compactSpacing,
        isCollapsed = collapsedStates[itemKey] == true,
        onToggleCollapse = {
            updateCollapsedState(
                collapsedStates = collapsedStates,
                key = itemKey,
                title = section.title
            )
        },
        scheduleExtraLabelProvider = scheduleExtraLabelProvider,
        horizontalPadding = horizontalPadding,
        bottomPadding = bottomPadding
    )
}

@Composable
internal fun TwoColumnScheduleSectionGrid(
    sections: List<ScheduleSectionDescriptor>,
    orderKey: String = "default",
    showOnlyUpcoming: Boolean,
    collapsedStates: MutableMap<String, Boolean> = mutableMapOf(),
    swapScheduleColumns: Boolean = false,
    compactSpacing: Boolean = false,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)? = null,
    layout: ScheduleResponsiveLayoutSpec,
    containerHorizontalPadding: Dp = 0.dp,
    horizontalPadding: Dp = 4.dp,
    columnSpacing: Dp = 4.dp,
    sectionSpacing: Dp = 8.dp
) {
    val orderedSections = sections.filter { it.schedules.isNotEmpty() }
    if (orderedSections.isEmpty()) return

    val splitIndex = (orderedSections.size + 1) / 2
    val firstColumnSections = orderedSections.take(splitIndex)
    val secondColumnSections = orderedSections.drop(splitIndex)
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            "schedule_column_order",
            android.content.Context.MODE_PRIVATE
        )
    }
    var userSwapped by remember(orderKey) {
        mutableStateOf(
            preferences.getBoolean(
                orderKey,
                false
            )
        )
    }
    val haptic = LocalHapticFeedback.current
    val columnDividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)
    val density = LocalDensity.current
    val previewThreshold = with(density) { 30.dp.toPx() }
    val dividerWidth = with(density) { (1.dp + columnSpacing * 2).toPx() }
    var draggedColumn by remember(orderKey) { mutableStateOf<Boolean?>(null) }
    var dragX by remember(orderKey) { mutableFloatStateOf(0f) }
    var columnWidth by remember(orderKey) { mutableFloatStateOf(0f) }

    run {
        val swapped = userSwapped
        val reverse = swapScheduleColumns.xor(swapped)
        val leftColumnSections = if (reverse) secondColumnSections else firstColumnSections
        val rightColumnSections = if (reverse) firstColumnSections else secondColumnSections
        val columnDrag: @Composable (Boolean) -> Modifier = { isLeft ->
            val showSwap = (draggedColumn == true && dragX > previewThreshold) ||
                    (draggedColumn == false && dragX < -previewThreshold)
            val previewOffset by animateFloatAsState(
                targetValue = if (showSwap && draggedColumn != isLeft) {
                    if (isLeft) columnWidth + dividerWidth else -columnWidth - dividerWidth
                } else 0f,
                animationSpec = if (draggedColumn == null) snap() else spring(),
                label = "schedule column preview"
            )
            Modifier
                .onSizeChanged { columnWidth = it.width.toFloat() }
                .pointerInput(orderKey, swapped, isLeft) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            draggedColumn = isLeft
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f; draggedColumn = null },
                        onDragEnd = {
                            if ((isLeft && dragX > 30.dp.toPx()) || (!isLeft && dragX < -30.dp.toPx())) {
                                userSwapped = !swapped
                                preferences.edit().putBoolean(orderKey, userSwapped).apply()
                            }
                            dragX = 0f
                            draggedColumn = null
                        },
                        onDrag = { change, amount -> change.consume(); dragX += amount.x }
                    )
                }
                .zIndex(if (draggedColumn == isLeft) 1f else 0f)
                .offset {
                    IntOffset(
                        when {
                            draggedColumn == null -> 0
                            draggedColumn == isLeft -> dragX.roundToInt()
                            else -> previewOffset.roundToInt()
                        }, 0
                    )
                }
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = containerHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(columnSpacing),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .then(columnDrag(true)),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            leftColumnSections.forEach { section ->
                ScheduleSectionCard(
                    section = section,
                    showOnlyUpcoming = showOnlyUpcoming,
                    collapsedStates = collapsedStates,
                    compactSpacing = compactSpacing,
                    scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                    layout = layout,
                    horizontalPadding = horizontalPadding
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .padding(vertical = 1.dp)
                .drawBehind {
                    val strokeWidth = 1f
                    val x = size.width / 2f
                    drawLine(
                        color = columnDividerColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .then(columnDrag(false)),
            verticalArrangement = Arrangement.spacedBy(sectionSpacing)
        ) {
            rightColumnSections.forEach { section ->
                ScheduleSectionCard(
                    section = section,
                    showOnlyUpcoming = showOnlyUpcoming,
                    collapsedStates = collapsedStates,
                    compactSpacing = compactSpacing,
                    scheduleExtraLabelProvider = scheduleExtraLabelProvider,
                    layout = layout,
                    horizontalPadding = horizontalPadding
                )
            }
        }
    }
    }
}

@Composable
private fun ScheduleSectionCard(
    section: ScheduleSectionDescriptor,
    showOnlyUpcoming: Boolean,
    collapsedStates: MutableMap<String, Boolean>,
    compactSpacing: Boolean,
    scheduleExtraLabelProvider: ((BusSchedule) -> String?)?,
    layout: ScheduleResponsiveLayoutSpec,
    horizontalPadding: Dp
) {
    val key = "two_column_${section.key}"
    val isCollapsed = collapsedStates[key] == true

    SingleColumnScheduleList(
        title = section.title,
        schedules = section.schedules,
        nextUpcomingId = section.nextUpcomingId,
        layout = layout,
        isCollapsed = isCollapsed,
        onToggleCollapse = {
            updateCollapsedState(
                collapsedStates = collapsedStates,
                key = key,
                title = section.title
            )
        },
        showOnlyUpcoming = showOnlyUpcoming,
        compactSpacing = compactSpacing,
        extraLabelProvider = scheduleExtraLabelProvider,
        modifier = Modifier.padding(horizontal = horizontalPadding)
    )
}

private fun updateCollapsedState(
    collapsedStates: MutableMap<String, Boolean>,
    key: String,
    title: String
) {
    val shouldCollapse = collapsedStates[key] != true
    collapsedStates[key] = shouldCollapse
    UserActionLogger.scheduleSectionToggled(title, shouldCollapse)
}

@Composable
internal fun NoScheduleMessage(
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.schedule_missing_for_selected_route),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = layout.emptyState.emptyStateHorizontalPadding)
        )
    }
}
