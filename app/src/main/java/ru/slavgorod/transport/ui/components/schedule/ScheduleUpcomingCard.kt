package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import org.json.JSONArray
import ru.slavgorod.transport.R
import kotlin.math.roundToInt

@Composable
internal fun UpcomingSchedulesCard(
    entries: List<UpcomingScheduleEntry>,
    currentTimeMillis: Long,
    layout: ScheduleResponsiveLayoutSpec,
    dayLabel: String? = null,
    compactMode: Boolean = false,
    squareCorners: Boolean = false,
    attachedToHeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val cardShape = upcomingSchedulesCardShape(
        compactMode = compactMode,
        squareCorners = squareCorners,
        attachedToHeader = attachedToHeader
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = UpcomingScheduleBoardColors.default().board),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        UpcomingSchedulesContent(
            entries = entries,
            currentTimeMillis = currentTimeMillis,
            layout = layout,
            dayLabel = dayLabel,
            compactMode = compactMode,
            squareCorners = squareCorners || attachedToHeader,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Suppress("CyclomaticComplexMethod")
@Composable
internal fun UpcomingSchedulesContent(
    entries: List<UpcomingScheduleEntry>,
    currentTimeMillis: Long,
    layout: ScheduleResponsiveLayoutSpec,
    dayLabel: String? = null,
    compactMode: Boolean = false,
    squareCorners: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            "upcoming_order",
            android.content.Context.MODE_PRIVATE
        )
    }
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    var draggedRow by remember { mutableStateOf<String?>(null) }
    var hoveredRow by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val orderKey = remember(entries) { JSONArray(entries.map { it.title }.sorted()).toString() }
    var order by remember(orderKey) {
        val saved = runCatching {
            val array = JSONArray(preferences.getString(orderKey, "[]"))
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList())
        mutableStateOf(saved.filter { name -> entries.any { it.title == name } } + entries.map { it.title }
            .filterNot { it in saved })
    }

    BoxWithConstraints(modifier = modifier) {
        val colors = UpcomingScheduleBoardColors.default()
        val cardWidth = this.maxWidth
        val isTightLayout = cardWidth < 320.dp || compactMode
        val isCompactLayout = cardWidth < 380.dp || compactMode
        val stackedRows = (cardWidth - layout.upcoming.upcomingCardContentPadding * 2) /
            LocalDensity.current.fontScale < 300.dp
        val cardContentPadding = layout.upcoming.upcomingCardContentPadding
        val contentSpacing = layout.section.sectionVerticalSpacing

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = cardContentPadding,
                    top = layout.upcoming.upcomingCardTopPadding,
                    end = cardContentPadding,
                    bottom = layout.upcoming.upcomingCardBottomPadding
                ),
            verticalArrangement = Arrangement.spacedBy(layout.section.sectionVerticalSpacing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = layout.upcoming.upcomingMinRowHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(if (isTightLayout) 16.dp else if (isCompactLayout) 18.dp else 22.dp),
                    tint = colors.content
                )
                Text(
                    text = if (dayLabel == null) {
                        stringResource(R.string.schedule_upcoming_departures_title)
                    } else {
                        stringResource(R.string.schedule_upcoming_departures_title_for_day, dayLabel)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = layout.upcoming.upcomingHeaderFontSize,
                        lineHeight = layout.upcoming.upcomingHeaderFontSize * 1.12f
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.content,
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (isTightLayout) 3.dp else 6.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
                if (!stackedRows) UpcomingScheduleColumnHeaders(
                    colors = colors,
                    textSpec = UpcomingScheduleTextSpec(
                        primaryTextSize = layout.upcoming.upcomingPrimaryTextSize,
                        supportingTextSize = layout.upcoming.upcomingSupportingTextSize
                    ),
                    fullWidthMode = squareCorners,
                    compactMode = compactMode
                )
                run {
                    val displayedOrder = order
                    Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
                        @Composable
                        fun ScheduleEntry(index: Int, entry: UpcomingScheduleEntry) {
                    val nextCountdownLabel = resolveDepartureMillis(
                        departureTime = entry.nextSchedule.departureTime,
                        currentTimeMillis = currentTimeMillis
                    )?.let { departureMillis ->
                        buildScheduleTimerLabel(departureMillis - currentTimeMillis)
                    }
                        ?: stringResource(R.string.schedule_time_soon)
                    val inTransitLabel = resolveElapsedDepartureMillis(
                        departureTime = entry.inTransitSchedule.departureTime,
                        currentTimeMillis = currentTimeMillis
                    )?.let { elapsedMillis ->
                        buildScheduleTimerLabel(elapsedMillis)
                    }

                                val sourceIndex = displayedOrder.indexOf(draggedRow)
                                val targetIndex = displayedOrder.indexOf(hoveredRow)
                                val previewIndex = reorderPreviewIndex(sourceIndex, targetIndex, index)
                                val ownBounds = rowBounds[entry.title]
                                val previewBounds =
                                    displayedOrder.getOrNull(previewIndex)?.let { rowBounds[it] }
                                val previewOffset by animateFloatAsState(
                                    targetValue = if (entry.title != draggedRow && ownBounds != null && previewBounds != null) {
                                        previewBounds.top - ownBounds.top
                                    } else 0f,
                                    animationSpec = if (draggedRow == null) snap() else spring(),
                                    label = "upcoming departure preview"
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            rowBounds[entry.title] = it.boundsInRoot()
                                        }
                                        .pointerInput(entry.title, displayedOrder) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    hapticFeedback.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    draggedRow = entry.title
                                                    hoveredRow = entry.title
                                                    dragOffset = 0f
                                                },
                                                onDragEnd = {
                                                    val destination =
                                                        displayedOrder.indexOf(hoveredRow)
                                                            .takeIf { it >= 0 } ?: index
                                                    draggedRow = null
                                                    hoveredRow = null
                                                    dragOffset = 0f
                                                    if (destination != index) {
                                                        order =
                                                            displayedOrder.toMutableList().apply {
                                                                add(
                                                                    destination,
                                                                    removeAt(index)
                                                                )
                                                            }
                                                        preferences.edit().putString(
                                                            orderKey,
                                                            JSONArray(order).toString()
                                                        ).apply()
                                                    }
                                                },
                                                onDragCancel = {
                                                    dragOffset = 0f; draggedRow = null; hoveredRow =
                                                    null
                                                },
                                                onDrag = { change, amount ->
                                                    change.consume()
                                                    dragOffset += amount.y
                                                    val startY = rowBounds[entry.title]?.center?.y
                                                    if (startY != null) {
                                                        hoveredRow = displayedOrder.filter {
                                                            rowBounds.containsKey(it)
                                                        }
                                                            .minByOrNull {
                                                                kotlin.math.abs(
                                                                    rowBounds.getValue(
                                                                        it
                                                                    ).center.y - (startY + dragOffset)
                                                                )
                                                            }
                                                            ?: entry.title
                                                    }
                                                }
                                            )
                                        }
                                        .zIndex(if (draggedRow == entry.title) 1f else 0f)
                                        .offset {
                                            IntOffset(
                                                0, when {
                                                    draggedRow == null -> 0
                                                    draggedRow == entry.title -> dragOffset.roundToInt()
                                                    else -> previewOffset.roundToInt()
                                                }
                                            )
                                        }
                                        .background(colors.board)) {
                    UpcomingScheduleBoardRow(
                        title = entry.title,
                        inTransitTime = entry.inTransitSchedule.departureTime,
                        inTransitLabel = inTransitLabel,
                        nextDepartureTime = entry.nextSchedule.departureTime,
                        nextCountdownLabel = nextCountdownLabel,
                        colors = colors,
                        textSpec = UpcomingScheduleTextSpec(
                            primaryTextSize = layout.upcoming.upcomingPrimaryTextSize,
                            supportingTextSize = layout.upcoming.upcomingSupportingTextSize
                        ),
                        fullWidthMode = squareCorners,
                        compactMode = compactMode
                    )
                                }

                                if (index != displayedOrder.lastIndex) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = colors.divider,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                        }
                        displayedOrder.mapNotNull { name -> entries.find { it.title == name } }
                            .forEachIndexed { index, entry -> ScheduleEntry(index, entry) }
                    }
                }
            }
        }
    }
}

private fun upcomingSchedulesCardShape(
    compactMode: Boolean,
    squareCorners: Boolean,
    attachedToHeader: Boolean
) = when {
    compactMode || squareCorners || attachedToHeader -> RoundedCornerShape(0.dp)
    else -> RoundedCornerShape(22.dp)
}

private data class UpcomingScheduleBoardColors(
    val board: Color,
    val content: Color,
    val divider: Color
) {
    companion object {
        fun default() = UpcomingScheduleBoardColors(
            board = Color(0xFF103B80),
            content = Color.White,
            divider = Color(0xFF5F91E6).copy(alpha = 0.46f)
        )
    }
}

private data class UpcomingScheduleTextSpec(
    val primaryTextSize: TextUnit,
    val supportingTextSize: TextUnit
)

private data class UpcomingScheduleRowSpec(
    val isTight: Boolean,
    val isCompact: Boolean,
    val minHeight: Dp,
    val titleTextSize: TextUnit,
    val timeTextSize: TextUnit,
    val timeLabelTextSize: TextUnit,
    val timeColumnsWeight: Float
)

private fun resolveUpcomingScheduleRowSpec(
    availableWidth: Dp,
    compactMode: Boolean,
    fullWidthMode: Boolean,
    textSpec: UpcomingScheduleTextSpec
): UpcomingScheduleRowSpec {
    val isTight = availableWidth < 300.dp || compactMode || fullWidthMode
    val isCompact = availableWidth < 380.dp || compactMode || fullWidthMode
    return UpcomingScheduleRowSpec(
        isTight = isTight,
        isCompact = isCompact,
        minHeight = when {
            isTight -> 34.dp
            isCompact -> 38.dp
            else -> 56.dp
        },
        titleTextSize = when {
            isTight -> textSpec.primaryTextSize * 0.94f
            isCompact -> textSpec.primaryTextSize * 1.02f
            else -> textSpec.primaryTextSize * 1.08f
        },
        timeTextSize = when {
            isTight -> textSpec.primaryTextSize * 1.02f
            isCompact -> textSpec.primaryTextSize * 1.1f
            else -> textSpec.primaryTextSize * 1.18f
        },
        timeLabelTextSize = when {
            isTight -> textSpec.supportingTextSize.value.coerceAtLeast(11f).sp
            isCompact -> textSpec.supportingTextSize.value.coerceAtLeast(11f).sp
            else -> textSpec.supportingTextSize
        },
        timeColumnsWeight = when {
            isTight -> 1.22f
            isCompact -> 1.12f
            else -> 1f
        }
    )
}

@Composable
private fun UpcomingScheduleColumnHeaders(
    colors: UpcomingScheduleBoardColors,
    textSpec: UpcomingScheduleTextSpec,
    fullWidthMode: Boolean,
    compactMode: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = this.maxWidth
        val fontScale = LocalDensity.current.fontScale
        val labels: @Composable () -> Unit = {
            Row(modifier = Modifier.fillMaxWidth()) {
                UpcomingScheduleColumnHeader(
                    text = stringResource(R.string.schedule_in_transit_column),
                    color = colors.content,
                    textSize = textSpec.supportingTextSize,
                    modifier = Modifier.weight(1f)
                )
                UpcomingScheduleColumnHeader(
                    text = stringResource(R.string.schedule_next_departure_column),
                    color = colors.content,
                    textSize = textSpec.supportingTextSize,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (availableWidth / fontScale < 300.dp) {
            labels()
        } else {
            val rowSpec = resolveUpcomingScheduleRowSpec(
                availableWidth = availableWidth,
                compactMode = compactMode,
                fullWidthMode = fullWidthMode,
                textSpec = textSpec
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.weight(rowSpec.timeColumnsWeight)) {
                    labels()
                }
            }
        }
    }
}

@Composable
private fun UpcomingScheduleColumnHeader(
    text: String,
    color: Color,
    textSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            fontSize = textSize,
            lineHeight = textSize * 1.1f,
            fontWeight = FontWeight.Bold
        ),
        color = color.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}

@Composable
private fun UpcomingScheduleBoardRow(
    title: String,
    inTransitTime: String,
    inTransitLabel: String?,
    nextDepartureTime: String,
    nextCountdownLabel: String,
    colors: UpcomingScheduleBoardColors,
    textSpec: UpcomingScheduleTextSpec,
    fullWidthMode: Boolean,
    compactMode: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = this.maxWidth
        val fontScale = LocalDensity.current.fontScale
        if (availableWidth / fontScale < 300.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                UpcomingPointSummary(title, colors, textSpec.primaryTextSize)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UpcomingTimeColumn(
                        inTransitTime, inTransitLabel.orEmpty(), colors.content,
                        textSpec.primaryTextSize, textSpec.supportingTextSize.value.coerceAtLeast(11f).sp, Modifier.weight(1f),
                        heading = stringResource(R.string.schedule_in_transit_column)
                    )
                    UpcomingTimeColumn(
                        nextDepartureTime, nextCountdownLabel, colors.content,
                        textSpec.primaryTextSize, textSpec.supportingTextSize.value.coerceAtLeast(11f).sp, Modifier.weight(1f),
                        heading = stringResource(R.string.schedule_next_departure_column)
                    )
                }
            }
            return@BoxWithConstraints
        }
        val rowSpec = resolveUpcomingScheduleRowSpec(
            availableWidth = availableWidth,
            compactMode = compactMode,
            fullWidthMode = fullWidthMode,
            textSpec = textSpec
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = rowSpec.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpcomingPointSummary(
                title = title,
                colors = colors,
                titleTextSize = rowSpec.titleTextSize,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (rowSpec.isTight) 6.dp else 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(rowSpec.timeColumnsWeight)
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpcomingTimeColumn(
                        time = inTransitTime,
                        label = inTransitLabel.orEmpty(),
                        bodyColor = colors.content,
                        timeTextSize = rowSpec.timeTextSize,
                        labelTextSize = rowSpec.timeLabelTextSize,
                        modifier = Modifier.weight(1f)
                    )
                    UpcomingTimeColumn(
                        time = nextDepartureTime,
                        label = nextCountdownLabel,
                        bodyColor = colors.content,
                        timeTextSize = rowSpec.timeTextSize,
                        labelTextSize = rowSpec.timeLabelTextSize,
                        modifier = Modifier.weight(1f)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(1.dp)
                        .height(40.dp)
                        .background(colors.divider)
                )
            }
        }
    }
}

@Composable
private fun UpcomingPointSummary(
    title: String,
    colors: UpcomingScheduleBoardColors,
    titleTextSize: TextUnit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = titleTextSize,
                    lineHeight = titleTextSize * 1.08f,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = colors.content,
                softWrap = true,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Clip,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun UpcomingTimeColumn(
    time: String,
    label: String,
    bodyColor: Color,
    timeTextSize: TextUnit,
    labelTextSize: TextUnit,
    modifier: Modifier = Modifier,
    heading: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (heading != null) {
            UpcomingScheduleColumnHeader(
                text = heading,
                color = bodyColor,
                textSize = labelTextSize.value.coerceAtLeast(11f).sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )
        }
        Text(
            text = time,
            style = MaterialTheme.typography.displaySmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                fontSize = timeTextSize,
                lineHeight = timeTextSize * 1.0f,
                fontWeight = FontWeight.ExtraBold
            ),
            color = bodyColor,
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(if (labelTextSize.value <= 11f) 0.dp else 2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                ),
                fontSize = labelTextSize,
                lineHeight = labelTextSize * 1.1f,
                fontWeight = FontWeight.SemiBold
            ),
            color = bodyColor.copy(alpha = 0.78f),
            maxLines = Int.MAX_VALUE,
            softWrap = true,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun reorderPreviewIndex(sourceIndex: Int, targetIndex: Int, index: Int): Int = when {
    sourceIndex < 0 || targetIndex < 0 -> index
    index == sourceIndex -> targetIndex
    sourceIndex < targetIndex && index in (sourceIndex + 1)..targetIndex -> index - 1
    sourceIndex > targetIndex && index in targetIndex until sourceIndex -> index + 1
    else -> index
}
