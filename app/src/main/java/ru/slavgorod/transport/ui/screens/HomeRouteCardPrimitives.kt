package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.BusRouteCard

@Composable
internal fun RouteCard(
    route: BusRoute,
    isGridMode: Boolean,
    gridColumns: Int,
    onClick: () -> Unit
) {
    BusRouteCard(
        route = route,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        isGridMode = isGridMode,
        gridColumns = gridColumns,
        onClick = onClick
    )
}
