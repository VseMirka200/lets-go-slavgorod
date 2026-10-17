package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val gridColumns = run {
        val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
        displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(initialValue = 2).value
    }
    RoutesListStateContent(
        routes = routes,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns
    )
}

@Composable
internal fun RoutesListStateForTest(
    routes: List<BusRoute>,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int
) {
    RoutesListStateContent(
        routes = routes,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns
    )
}

@Composable
private fun RoutesListStateContent(
    routes: List<BusRoute>,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int
) {
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
            addRoutes(
                routes = routes,
                gridColumns = gridColumns,
                isGridMode = false,
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
            addRoutes(
                routes = routes,
                gridColumns = gridColumns,
                isGridMode = true,
                navController = navController
            )
        }
    }
}

private fun LazyListScope.addRoutes(
    routes: List<BusRoute>,
    gridColumns: Int,
    isGridMode: Boolean,
    navController: NavController
) {
    items(items = routes, key = BusRoute::id) { route ->
        RouteCard(
            route = route,
            isGridMode = isGridMode,
            gridColumns = gridColumns,
            onClick = { navigateToSchedule(navController, route) }
        )
    }
}

private fun LazyGridScope.addRoutes(
    routes: List<BusRoute>,
    gridColumns: Int,
    isGridMode: Boolean,
    navController: NavController
) {
    items(items = routes, key = BusRoute::id) { route ->
        RouteCard(
            route = route,
            isGridMode = isGridMode,
            gridColumns = gridColumns,
            onClick = { navigateToSchedule(navController, route) }
        )
    }
}
