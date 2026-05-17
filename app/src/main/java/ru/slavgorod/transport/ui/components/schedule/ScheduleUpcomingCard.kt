package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R

@Composable
internal fun UpcomingSchedulesCard(
    entries: List<UpcomingScheduleEntry>,
    currentTimeMillis: Long,
    layout: ScheduleResponsiveLayoutSpec,
    compactMode: Boolean = false,
    squareCorners: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    BoxWithConstraints(modifier = modifier) {
        val colors = UpcomingScheduleBoardColors.default()
        val cardWidth = this.maxWidth
        val isTightLayout = cardWidth < 320.dp || compactMode
        val isCompactLayout = cardWidth < 380.dp || compactMode
        val cardContentPadding = layout.upcoming.upcomingCardContentPadding
        val contentSpacing = layout.section.sectionVerticalSpacing

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = if (compactMode || squareCorners) RoundedCornerShape(0.dp) else RoundedCornerShape(
                22.dp
            ),
            colors = CardDefaults.cardColors(containerColor = colors.board),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
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
                        text = stringResource(R.string.schedule_upcoming_departures_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = layout.upcoming.upcomingHeaderFontSize,
                            lineHeight = layout.upcoming.upcomingHeaderFontSize * 1.12f
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (isTightLayout) 3.dp else 6.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(contentSpacing)) {
                    entries.forEachIndexed { index, entry ->
                        val diffMinutes = resolveDepartureDiffMinutes(
                            departureTime = entry.schedule.departureTime,
                            currentTimeMillis = currentTimeMillis
                        )
                        val minutesLabel = diffMinutes?.let { buildCountdownLabel(it) }
                            ?: stringResource(R.string.schedule_time_soon)

                        UpcomingScheduleBoardRow(
                            title = entry.title,
                            countdownLabel = minutesLabel,
                            departureTime = entry.schedule.departureTime,
                            nextDepartureTime = entry.nextDepartureTime,
                            colors = colors,
                            textSpec = UpcomingScheduleTextSpec(
                                primaryTextSize = layout.upcoming.upcomingPrimaryTextSize,
                                supportingTextSize = layout.upcoming.upcomingSupportingTextSize
                            ),
                            fullWidthMode = squareCorners,
                            compactMode = compactMode
                        )

                        if (index != entries.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.5.dp,
                                color = colors.divider,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class UpcomingScheduleBoardColors(
    val board: Color,
    val content: Color,
    val divider: Color,
    val bubble: Color
) {
    companion object {
        fun default() = UpcomingScheduleBoardColors(
            board = Color(0xFF0D47A1),
            content = Color.White,
            divider = Color(0xFF5F91E6).copy(alpha = 0.46f),
            bubble = Color(0xFF1D63CB)
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
            isTight -> textSpec.supportingTextSize * 0.85f
            isCompact -> textSpec.supportingTextSize * 0.92f
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
private fun UpcomingScheduleBoardRow(
    title: String,
    countdownLabel: String,
    departureTime: String,
    nextDepartureTime: String?,
    colors: UpcomingScheduleBoardColors,
    textSpec: UpcomingScheduleTextSpec,
    fullWidthMode: Boolean,
    compactMode: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = this.maxWidth
        val rowSpec = resolveUpcomingScheduleRowSpec(
            availableWidth = availableWidth,
            compactMode = compactMode,
            fullWidthMode = fullWidthMode,
            textSpec = textSpec
        )
        val resolvedNextDepartureTime = nextDepartureTime?.takeIf(String::isNotBlank)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = rowSpec.minHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpcomingPointSummary(
                title = title,
                countdownLabel = countdownLabel,
                colors = colors,
                titleTextSize = rowSpec.titleTextSize,
                countdownTextSize = textSpec.supportingTextSize,
                compactMode = rowSpec.isCompact,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (rowSpec.isTight) 6.dp else 8.dp)
            )

            Box(
                modifier = Modifier
                    .weight(rowSpec.timeColumnsWeight)
                    .fillMaxHeight()
                    .align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpcomingTimeColumn(
                        time = departureTime,
                        label = stringResource(R.string.schedule_departure_label),
                        bodyColor = colors.content,
                        timeTextSize = rowSpec.timeTextSize,
                        labelTextSize = rowSpec.timeLabelTextSize,
                        modifier = Modifier.weight(1f)
                    )
                    resolvedNextDepartureTime?.let { nextTime ->
                        UpcomingTimeColumn(
                            time = nextTime,
                            label = stringResource(R.string.schedule_next_departure_label),
                            bodyColor = colors.content,
                            timeTextSize = rowSpec.timeTextSize,
                            labelTextSize = rowSpec.timeLabelTextSize,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (resolvedNextDepartureTime != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(colors.divider)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingPointSummary(
    title: String,
    countdownLabel: String,
    colors: UpcomingScheduleBoardColors,
    titleTextSize: TextUnit,
    countdownTextSize: TextUnit,
    compactMode: Boolean,
    modifier: Modifier = Modifier
) {
    val countdownBadgeHorizontalPadding = if (compactMode) 5.dp else 7.dp

    Row(
        modifier = modifier.fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
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
            Spacer(modifier = Modifier.height(if (compactMode) 3.dp else 5.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.bubble)
                    .padding(
                        horizontal = countdownBadgeHorizontalPadding,
                        vertical = if (compactMode) 1.dp else 2.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = colors.content,
                    modifier = Modifier.size(if (compactMode) 12.dp else 14.dp)
                )
                Text(
                    text = countdownLabel,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = countdownTextSize,
                        lineHeight = countdownTextSize * 1.08f,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.content,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.padding(start = if (compactMode) 2.dp else 4.dp)
                )
            }
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            maxLines = 1,
            softWrap = false,
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
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
