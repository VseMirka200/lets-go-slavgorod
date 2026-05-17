package ru.slavgorod.transport.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.data.model.BusSchedule
import ru.slavgorod.transport.ui.components.schedule.ScheduleResponsiveLayoutSpec
import ru.slavgorod.transport.ui.components.schedule.extractDayLabel

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
internal fun CompactScheduleCard(
    schedule: BusSchedule,
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier,
    isNextUpcoming: Boolean = false,
    showDayLabel: Boolean = false,
    extraLabel: String? = null,
    showBottomDivider: Boolean = true
) {
    val resolvedNotesLabel = extraLabel
        ?: schedule.notes
            ?.trim()
            ?.takeIf(String::isNotBlank)
    val resolvedHorizontalPadding = layout.card.cardHorizontalPadding
    val resolvedVerticalPadding = layout.card.cardVerticalPadding * 0.9f
    val contentSpacing = layout.card.cardInnerSpacing
    val hasSupportingContent = resolvedNotesLabel != null || showDayLabel

    val containerColor = if (isNextUpcoming) {
        Color(0xFFFFD54F).copy(alpha = 0.4f)
    } else {
        Color.Transparent
    }
    val highlightTextColor = if (isNextUpcoming) {
        Color(0xFF1F1F1F)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
    }
    val titleColor = if (isNextUpcoming) {
        Color(0xFF1F1F1F)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    var isDescriptionExpanded by remember(schedule.id) { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(
                    horizontal = resolvedHorizontalPadding,
                    vertical = resolvedVerticalPadding
                )
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = schedule.departureTime,
                    style = MaterialTheme.typography.titleMedium.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        fontWeight = if (isNextUpcoming) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = layout.card.departureTimeFontSize,
                        lineHeight = layout.card.departureTimeFontSize
                    ),
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (hasSupportingContent) {
                Spacer(modifier = Modifier.height(2.dp))

                val descriptionStyle = MaterialTheme.typography.bodyMedium.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    fontSize = layout.card.supportingTextFontSize,
                    lineHeight = layout.card.supportingTextFontSize * 1.15f,
                    fontWeight = FontWeight.Medium
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val textMeasurer = rememberTextMeasurer()
                    val availableWidthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
                    val measuredLayout = resolvedNotesLabel?.let { label ->
                        textMeasurer.measure(
                            text = AnnotatedString(label),
                            style = descriptionStyle,
                            constraints = Constraints(maxWidth = availableWidthPx)
                        )
                    }
                    val showCollapseToggle = measuredLayout?.lineCount?.let { it > 1 } == true

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = showCollapseToggle,
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isDescriptionExpanded = !isDescriptionExpanded
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        resolvedNotesLabel?.let { label ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = descriptionStyle,
                                    color = highlightTextColor,
                                    softWrap = true,
                                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        if (showCollapseToggle) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Icon(
                                imageVector = Icons.Outlined.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (showDayLabel) {
                    Spacer(modifier = Modifier.height(contentSpacing))
                    extractDayLabel(schedule)?.let { dayLabel ->
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.bodySmall.copy(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            color = highlightTextColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        if (showBottomDivider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
