package ru.slavgorod.transport.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.slavgorod.transport.data.model.BusRoute

@Composable
fun BusRouteCard(
    route: BusRoute,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    isPinned: Boolean = false,
    modifier: Modifier = Modifier,
    isGridMode: Boolean = true,
    gridColumns: Int = 2
) {
    if (isGridMode) {
        BusRouteCardGrid(
            route = route,
            onRouteClick = onClick,
            onRouteLongPress = onLongPress,
            isPinned = isPinned,
            modifier = modifier,
            gridColumns = gridColumns
        )
    } else {
        BusRouteCardList(
            route = route,
            onRouteClick = onClick,
            onRouteLongPress = onLongPress,
            isPinned = isPinned,
            modifier = modifier
        )
    }
}
