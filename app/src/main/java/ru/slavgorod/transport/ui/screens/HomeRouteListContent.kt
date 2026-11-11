package ru.slavgorod.transport.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import org.json.JSONArray
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel
import kotlin.math.roundToInt

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
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(
            "route_card_order",
            android.content.Context.MODE_PRIVATE
        )
    }
    var pinned by remember {
        mutableStateOf(
            preferences.getStringSet("pinned", emptySet())?.toSet() ?: emptySet()
        )
    }
    var menuRoute by remember { mutableStateOf<String?>(null) }
    var menuBounds by remember { mutableStateOf<Rect?>(null) }
    var listCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var order by remember {
        mutableStateOf(runCatching {
            val array = JSONArray(preferences.getString("order", "[]"))
            (0 until array.length()).map { array.getString(it) }
        }.getOrDefault(emptyList()))
    }
    val positions = remember { mutableStateMapOf<String, Rect>() }
    var draggedId by remember { mutableStateOf<String?>(null) }
    var hoveredId by remember { mutableStateOf<String?>(null) }
    var drag by remember { mutableStateOf(Offset.Zero) }
    val haptic = LocalHapticFeedback.current
    val orderedRoutes = routes.sortedBy { route ->
        order.indexOf(route.id).let { if (it < 0) Int.MAX_VALUE else it }
    }
    val pinnedRoutes = orderedRoutes.filter { it.id in pinned }
    val otherRoutes = orderedRoutes.filterNot { it.id in pinned }
    val moveRoute: (String, String) -> Unit = { from, to ->
        val visible = orderedRoutes.map { it.id }.toMutableList()
        val source = visible.indexOf(from)
        val target = visible.indexOf(to)
        if (source >= 0 && target >= 0 && source != target) {
            visible.add(target, visible.removeAt(source))
            val visibleSet = visible.toSet()
            order = visible + order.filterNot { it in visibleSet }
            preferences.edit().putString("order", JSONArray(order).toString()).apply()
        }
    }
    val dragModifier: @Composable (String) -> Modifier = { id ->
        DisposableEffect(id) { onDispose { positions.remove(id) } }
        val ids = orderedRoutes.filter { (it.id in pinned) == (id in pinned) }.map { it.id }
        val sourceIndex = ids.indexOf(draggedId)
        val targetIndex = ids.indexOf(hoveredId)
        val currentIndex = ids.indexOf(id)
        val previewIndex = when {
            sourceIndex < 0 || targetIndex < 0 -> currentIndex
            currentIndex == sourceIndex -> targetIndex
            sourceIndex < targetIndex && currentIndex in (sourceIndex + 1)..targetIndex -> currentIndex - 1
            sourceIndex > targetIndex && currentIndex in targetIndex until sourceIndex -> currentIndex + 1
            else -> currentIndex
        }
        val originalBounds = positions[id]
        val previewBounds = ids.getOrNull(previewIndex)?.let { positions[it] }
        val previewOffset =
            if (id != draggedId && originalBounds != null && previewBounds != null) {
                previewBounds.topLeft - originalBounds.topLeft
            } else Offset.Zero
        val animatedOffset by animateIntOffsetAsState(
            targetValue = IntOffset(previewOffset.x.roundToInt(), previewOffset.y.roundToInt()),
            animationSpec = if (draggedId == null) snap() else spring(),
            label = "route reorder preview"
        )
        Modifier
            .onGloballyPositioned { positions[id] = it.boundsInRoot() }
            .pointerInput(id, orderedRoutes) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        draggedId = id
                        hoveredId = id
                        menuRoute = id
                        drag = Offset.Zero
                    },
                    onDragCancel = { drag = Offset.Zero; draggedId = null; hoveredId = null },
                    onDragEnd = {
                        val destination = hoveredId
                        val wasTap = drag.getDistance() <= 8.dp.toPx()
                        draggedId = null
                        hoveredId = null
                        drag = Offset.Zero
                        if (wasTap) menuRoute = id
                        else if (destination != null && destination != id) moveRoute(
                            id,
                            destination
                        )
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                        if (drag.getDistance() > 8.dp.toPx()) menuRoute = null
                        val center = positions[id]?.center
                        val destination = center?.let { point ->
                            positions.entries.firstOrNull {
                                (it.key in pinned) == (id in pinned) && it.key in orderedRoutes.map { route -> route.id } && it.value.contains(
                                    point + drag
                                )
                            }?.key
                        }
                        if (destination != null) hoveredId = destination
                    }
                )
            }
            .zIndex(if (id == draggedId) 1f else 0f)
            .offset {
                when {
                    draggedId == null -> IntOffset.Zero
                    id == draggedId -> IntOffset(drag.x.roundToInt(), drag.y.roundToInt())
                    else -> animatedOffset
                }
            }
    }
    val routeMenu: @Composable (String) -> Unit = { id ->
        if (menuRoute == id) {
            Surface(
                modifier = Modifier.onGloballyPositioned { menuBounds = it.boundsInRoot() },
                onClick = {
                    pinned = if (id in pinned) pinned - id else pinned + id
                    preferences.edit().putStringSet("pinned", pinned).apply()
                    menuRoute = null
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    text = if (id in pinned) "Открепить" else "Закрепить",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    BackHandler(enabled = menuRoute != null) { menuRoute = null }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { listCoordinates = it }
            .pointerInput(menuRoute, menuBounds) {
                if (menuRoute != null) {
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        val point = listCoordinates?.localToRoot(down.position)
                        if (point != null && menuBounds?.contains(point) != true) menuRoute = null
                    }
                }
            }) {
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
                if (pinnedRoutes.isNotEmpty()) {
                    item(key = "pinned_routes_heading") { RouteGroupHeading("Закреплённые маршруты") }
                    addRoutes(
                        routes = pinnedRoutes,
                        gridColumns = resolvedColumns,
                        isGridMode = false,
                        onRouteClick = { route ->
                            if (menuRoute != route.id && draggedId == null) navigateToSchedule(
                                navController,
                                route
                            )
                        },
                        dragModifier = dragModifier,
                        routeMenu = routeMenu
                    )
                    if (otherRoutes.isNotEmpty()) item(key = "other_routes_heading") {
                        RouteGroupHeading(
                            "Другие маршруты"
                        )
                    }
                }
                addRoutes(
                    routes = otherRoutes,
                    gridColumns = resolvedColumns,
                    isGridMode = false,
                    onRouteClick = { route ->
                        if (menuRoute != route.id && draggedId == null) navigateToSchedule(
                            navController,
                            route
                        )
                    },
                    dragModifier = dragModifier,
                    routeMenu = routeMenu
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
                if (pinnedRoutes.isNotEmpty()) {
                    item(
                        key = "pinned_routes_heading",
                        span = { GridItemSpan(maxLineSpan) }) { RouteGroupHeading("Закреплённые маршруты") }
                    addRoutes(
                        routes = pinnedRoutes,
                        gridColumns = resolvedColumns,
                        isGridMode = true,
                        onRouteClick = { route ->
                            if (menuRoute != route.id && draggedId == null) navigateToSchedule(
                                navController,
                                route
                            )
                        },
                        dragModifier = dragModifier,
                        routeMenu = routeMenu
                    )
                    if (otherRoutes.isNotEmpty()) item(
                        key = "other_routes_heading",
                        span = { GridItemSpan(maxLineSpan) }) { RouteGroupHeading("Другие маршруты") }
                }
                addRoutes(
                    routes = otherRoutes,
                    gridColumns = resolvedColumns,
                    isGridMode = true,
                    onRouteClick = { route ->
                        if (menuRoute != route.id && draggedId == null) navigateToSchedule(
                            navController,
                            route
                        )
                    },
                    dragModifier = dragModifier,
                    routeMenu = routeMenu
                )
            }
        }
    }
}

@Composable
private fun RouteGroupHeading(title: String) {
    Text(
        title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        style = MaterialTheme.typography.titleMedium
    )
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.addRoutes(
    routes: List<BusRoute>,
    gridColumns: Int,
    isGridMode: Boolean,
    onRouteClick: (BusRoute) -> Unit,
    dragModifier: @Composable (String) -> Modifier,
    routeMenu: @Composable (String) -> Unit
) {
    items(items = routes, key = BusRoute::id) { route ->
        Box(modifier = dragModifier(route.id)) {
        RouteCard(
            route = route,
            isGridMode = isGridMode,
            gridColumns = gridColumns,
            onClick = { onRouteClick(route) }
        )
            Box(modifier = Modifier.align(Alignment.TopEnd)) { routeMenu(route.id) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyGridScope.addRoutes(
    routes: List<BusRoute>,
    gridColumns: Int,
    isGridMode: Boolean,
    onRouteClick: (BusRoute) -> Unit,
    dragModifier: @Composable (String) -> Modifier,
    routeMenu: @Composable (String) -> Unit
) {
    items(items = routes, key = BusRoute::id) { route ->
        Box(modifier = dragModifier(route.id)) {
        RouteCard(
            route = route,
            isGridMode = isGridMode,
            gridColumns = gridColumns,
            onClick = { onRouteClick(route) }
        )
            Box(modifier = Modifier.align(Alignment.TopEnd)) { routeMenu(route.id) }
        }
    }
}
