package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel

private val RouteCardsSpacing = 12.dp

@Composable
internal fun RoutesListState(
    routes: List<BusRoute>,
    pinnedRouteIds: Set<String>,
    onRoutePinActionReveal: (routeId: String) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val gridColumns = run {
        val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
        displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(initialValue = 2).value
    }
    RoutesListStateContent(
        routes = routes,
        pinnedRouteIds = pinnedRouteIds,
        onRoutePinActionReveal = onRoutePinActionReveal,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns
    )
}

@Composable
internal fun RoutesListStateForTest(
    routes: List<BusRoute>,
    pinnedRouteIds: Set<String>,
    onRoutePinActionReveal: (routeId: String) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int
) {
    RoutesListStateContent(
        routes = routes,
        pinnedRouteIds = pinnedRouteIds,
        onRoutePinActionReveal = onRoutePinActionReveal,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns
    )
}

@Composable
private fun RoutesListStateContent(
    routes: List<BusRoute>,
    pinnedRouteIds: Set<String>,
    onRoutePinActionReveal: (routeId: String) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int
) {
    val routeSections = buildRouteSections(routes, pinnedRouteIds)

    if (gridColumns == 1) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = RouteCardsSpacing,
                bottom = RouteCardsSpacing
            ),
            verticalArrangement = Arrangement.spacedBy(RouteCardsSpacing),
            state = listState
        ) {
            addRouteSections(
                routeSections = routeSections,
                pinnedRouteIds = pinnedRouteIds,
                gridColumns = gridColumns,
                isGridMode = false,
                onRoutePinActionReveal = onRoutePinActionReveal,
                navController = navController
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = RouteCardsSpacing,
                bottom = RouteCardsSpacing
            ),
            horizontalArrangement = Arrangement.spacedBy(RouteCardsSpacing),
            verticalArrangement = Arrangement.spacedBy(RouteCardsSpacing)
        ) {
            addRouteSections(
                routeSections = routeSections,
                pinnedRouteIds = pinnedRouteIds,
                gridColumns = gridColumns,
                isGridMode = true,
                onRoutePinActionReveal = onRoutePinActionReveal,
                navController = navController
            )
        }
    }
}

private fun LazyListScope.addRouteSections(
    routeSections: List<RouteSection>,
    pinnedRouteIds: Set<String>,
    gridColumns: Int,
    isGridMode: Boolean,
    onRoutePinActionReveal: (routeId: String) -> Unit,
    navController: NavController
) {
    routeSections.forEach { section ->
        item(key = "${section.keyPrefix}_header") {
            RoutesSectionHeader(title = stringResource(section.titleResId))
        }
        items(
            items = section.routes,
            key = { route -> "${section.keyPrefix}_${route.id}" }
        ) { route ->
            RouteCard(
                route = route,
                isPinned = section.pinned || route.id in pinnedRouteIds,
                isGridMode = isGridMode,
                gridColumns = gridColumns,
                onLongPress = onRoutePinActionReveal,
                onClick = { navigateToSchedule(navController, route) }
            )
        }
    }
}

private fun LazyGridScope.addRouteSections(
    routeSections: List<RouteSection>,
    pinnedRouteIds: Set<String>,
    gridColumns: Int,
    isGridMode: Boolean,
    onRoutePinActionReveal: (routeId: String) -> Unit,
    navController: NavController
) {
    routeSections.forEach { section ->
        item(span = { GridItemSpan(maxLineSpan) }, key = "${section.keyPrefix}_header") {
            RoutesSectionHeader(title = stringResource(section.titleResId))
        }
        items(
            items = section.routes,
            key = { route -> "${section.keyPrefix}_${route.id}" }
        ) { route ->
            RouteCard(
                route = route,
                isPinned = section.pinned || route.id in pinnedRouteIds,
                isGridMode = isGridMode,
                gridColumns = gridColumns,
                onLongPress = onRoutePinActionReveal,
                onClick = { navigateToSchedule(navController, route) }
            )
        }
    }
}
