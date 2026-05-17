package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.BusRouteCard

@Composable
internal fun RouteCard(
    route: BusRoute,
    isPinned: Boolean,
    isGridMode: Boolean,
    gridColumns: Int,
    onLongPress: (String) -> Unit,
    onClick: () -> Unit
) {
    BusRouteCard(
        route = route,
        isPinned = isPinned,
        isGridMode = isGridMode,
        gridColumns = gridColumns,
        onLongPress = { onLongPress(route.id) },
        onClick = onClick
    )
}

@Composable
internal fun RoutesSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}
