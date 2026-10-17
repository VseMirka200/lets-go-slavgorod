package ru.slavgorod.transport.ui.screens

import androidx.compose.runtime.Composable
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
        isGridMode = isGridMode,
        gridColumns = gridColumns,
        onClick = onClick
    )
}
