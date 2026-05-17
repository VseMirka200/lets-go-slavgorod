package ru.slavgorod.transport.ui.components.schedule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FilterList
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.components.settings.SettingsMenuRow
import ru.slavgorod.transport.ui.components.settings.SettingsRadioRow

@Composable
internal fun ScheduleFiltersMenuButton(
    layout: ScheduleResponsiveLayoutSpec,
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "filtersArrowRotation"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = layout.filters.filterSheetHorizontalPadding,
                    vertical = layout.filters.filterSheetVerticalSpacing + 4.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(layout.filters.filterSheetVerticalSpacing + 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (layout.mode == ScheduleResponsiveLayoutMode.TIGHT) 18.dp else 20.dp)
                )
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = layout.upcoming.upcomingHeaderFontSize * 0.92f,
                        lineHeight = layout.upcoming.upcomingHeaderFontSize
                    )
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = arrowRotation
                    }
                )
            }
        }
    }
}

@Composable
internal fun ScheduleFiltersSheetContent(
    layout: ScheduleResponsiveLayoutSpec,
    selectedDescriptionLabel: String?,
    descriptionLabels: List<String>,
    onDescriptionSelect: (String) -> Unit,
    onDescriptionClear: () -> Unit,
    selectedDeparturePointFilterId: String?,
    departurePointLabels: List<Pair<String, String>>,
    onDeparturePointSelect: (String) -> Unit,
    onDeparturePointClear: () -> Unit
) {
    var expandedSection by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = layout.filters.filterSheetHorizontalPadding, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(layout.filters.filterSheetVerticalSpacing)
    ) {
        ScheduleDeparturePointFilterSheetContent(
            selectedFilterId = selectedDeparturePointFilterId,
            labels = departurePointLabels,
            onSelect = onDeparturePointSelect,
            onClear = onDeparturePointClear,
            expanded = expandedSection == DEPARTURE_POINT_SECTION,
            onExpandedChange = { expanded ->
                expandedSection = expandedSection.toggleSection(
                    sectionKey = DEPARTURE_POINT_SECTION,
                    expanded = expanded
                )
            }
        )

        if (descriptionLabels.isNotEmpty()) {
            ScheduleDescriptionFilterSheetContent(
                selectedLabel = selectedDescriptionLabel,
                labels = descriptionLabels,
                onSelect = onDescriptionSelect,
                onClear = onDescriptionClear,
                expanded = expandedSection == DESCRIPTION_SECTION,
                onExpandedChange = { expanded ->
                    expandedSection = expandedSection.toggleSection(
                        sectionKey = DESCRIPTION_SECTION,
                        expanded = expanded
                    )
                }
            )
        }
    }
}

@Composable
private fun ScheduleDeparturePointFilterSheetContent(
    selectedFilterId: String?,
    labels: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ScheduleFilterSection(
        title = stringResource(R.string.schedule_filter_departures),
        selectedKey = selectedFilterId,
        selectedLabel = labels.firstOrNull { it.first == selectedFilterId }?.second
            ?: stringResource(R.string.schedule_filter_all),
        options = listOf(FilterOption(null, stringResource(R.string.schedule_filter_all))) +
                labels.map { (id, label) -> FilterOption(id, label) },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onOptionSelected = { selectedId ->
            if (selectedId == null) {
                onClear()
            } else {
                onSelect(selectedId)
            }
        }
    )
}

@Composable
private fun ScheduleDescriptionFilterSheetContent(
    selectedLabel: String?,
    labels: List<String>,
    onSelect: (String) -> Unit,
    onClear: () -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    ScheduleFilterSection(
        title = stringResource(R.string.schedule_filter_description),
        selectedKey = selectedLabel,
        selectedLabel = selectedLabel ?: stringResource(R.string.schedule_filter_all),
        options = listOf(FilterOption(null, stringResource(R.string.schedule_filter_all))) +
                labels.map { FilterOption(it, it) },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        onOptionSelected = { selectedId ->
            if (selectedId == null) {
                onClear()
            } else {
                onSelect(selectedId)
            }
        }
    )
}

@Composable
private fun ScheduleFilterSection(
    title: String,
    selectedKey: String?,
    selectedLabel: String,
    options: List<FilterOption>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOptionSelected: (String?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsMenuRow(
            title = title,
            subtitle = selectedLabel,
            onClick = { onExpandedChange(!expanded) },
            icon = Icons.Default.Settings,
            expanded = expanded,
            expandable = true
        )

        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.fillMaxWidth()) {
                options.forEach { option ->
                    SettingsRadioRow(
                        selected = selectedKey == option.key,
                        title = option.label,
                        onClick = {
                            onOptionSelected(option.key)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}

private fun String?.toggleSection(
    sectionKey: String,
    expanded: Boolean
): String? {
    return if (expanded) sectionKey else if (this == sectionKey) null else this
}

private data class FilterOption(
    val key: String?,
    val label: String
)

private const val DEPARTURE_POINT_SECTION = "departure_point"
private const val DESCRIPTION_SECTION = "description"
