package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
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
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    val gridColumns = run {
        val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
        displaySettingsViewModel.gridColumns.collectAsStateWithLifecycle(initialValue = 2).value
    }
    RoutesListStateContent(
        routes = routes,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns,
        header = header
    )
}

@Composable
internal fun RoutesListStateForTest(
    routes: List<BusRoute>,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int,
    topPadding: Dp = RouteCardsSpacing
) {
    RoutesListStateContent(
        routes = routes,
        navController = navController,
        modifier = modifier,
        gridColumns = gridColumns,
        header = null,
        topPadding = topPadding
    )
}

@Composable
private fun RoutesListStateContent(
    routes: List<BusRoute>,
    navController: NavController,
    modifier: Modifier = Modifier,
    gridColumns: Int,
    header: (@Composable () -> Unit)?,
    topPadding: Dp = if (header == null) RouteCardsSpacing else 0.dp
) {
    Box(modifier = modifier.fillMaxSize()) {
        val resolvedColumns = gridColumns.coerceIn(1, 4)
        if (resolvedColumns == 1) {
            val listState = rememberLazyListState()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = RouteCardsSpacing
                ),
                verticalArrangement = Arrangement.spacedBy(RouteCardsSpacing),
                state = listState
            ) {
                if (header != null) item(key = "search_header") { header() }
                addRoutes(
                    routes = routes,
                    gridColumns = resolvedColumns,
                    isGridMode = false,
                    navController = navController
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(resolvedColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = topPadding,
                    bottom = RouteCardsSpacing
                ),
                horizontalArrangement = Arrangement.spacedBy(RouteCardsSpacing),
                verticalArrangement = Arrangement.spacedBy(RouteCardsSpacing)
            ) {
                if (header != null) item(key = "search_header", span = { GridItemSpan(maxLineSpan) }) { header() }
                addRoutes(
                    routes = routes,
                    gridColumns = resolvedColumns,
                    isGridMode = true,
                    navController = navController
                )
            }
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
