package com.example.lets_go_slavgorod.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.lets_go_slavgorod.R
import com.example.lets_go_slavgorod.data.model.BusRoute
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.ui.components.BusRouteCard
import com.example.lets_go_slavgorod.ui.viewmodel.BusViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.ContextViewModelFactory
import com.example.lets_go_slavgorod.ui.viewmodel.DisplaySettingsViewModel
import com.example.lets_go_slavgorod.ui.viewmodel.RouteDisplayMode
import com.example.lets_go_slavgorod.ui.theme.DesignTokens
import timber.log.Timber

/**
 * Экран избранных времен отправления автобусов
 * 
 * Отображает все маршруты, для которых пользователь добавил
 * избранные времена отправления. Каждый маршрут показывается
 * в виде карточки с информацией о количестве избранных времен.
 * 
 * Основные функции:
 * - Отображение маршрутов с избранными временами
 * - Поддержка режимов отображения (сетка/список)
 * - Навигация к детальной информации маршрута
 * - Отображение пустого состояния при отсутствии избранного
 * - Группировка и сортировка маршрутов
 * 
 * @param viewModel ViewModel для управления данными избранного
 * @param navController контроллер навигации для перехода между экранами
 * @param modifier модификатор для настройки внешнего вида экрана
 * 
 * @author VseMirka200
 * @version 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteTimesScreen(
    viewModel: BusViewModel,
    navController: NavController? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favoriteTimesList by viewModel.favoriteTimes.collectAsState()
    val isLoading = false
    
    // Получаем настройки отображения
    val displaySettingsViewModel: DisplaySettingsViewModel = viewModel(
        factory = ContextViewModelFactory.create(context) { DisplaySettingsViewModel(it) }
    )
    val displayMode by displaySettingsViewModel.displayMode.collectAsState(initial = RouteDisplayMode.GRID)
    val gridColumns by displaySettingsViewModel.gridColumns.collectAsState(initial = 2)
    
    // Группируем избранные времена по маршрутам и сортируем по дате добавления
    val favoriteRoutes = remember(favoriteTimesList) {
        favoriteTimesList
            .groupBy { it.routeId }
            .map { (routeId, times) ->
                // Берем первое время для получения информации о маршруте
                val firstTime = times.first()
                // Получаем цвет для маршрута (используем те же цвета, что в BusRouteRepository)
                val routeColor = when (routeId) {
                    "102" -> "#FF6200EE"  // Фиолетовый
                    "102B" -> "#FF4CAF50" // Зелёный
                    "1" -> "#FF1976D2"    // Синий
                    else -> "#1976D2"     // По умолчанию синий
                }
                // Создаем виртуальный BusRoute для отображения
                BusRoute(
                    id = routeId,
                    routeNumber = firstTime.routeNumber,
                    name = firstTime.routeName,
                    description = "${times.size} избранных времен",
                    travelTime = null,
                    paymentMethods = null,
                    color = routeColor
                )
            }
            .sortedWith(compareBy<BusRoute> { route ->
                // Сортируем в том же порядке, что и основные маршруты: 102, 102B, 1, остальные
                when (route.id) {
                    "102" -> 1
                    "102B" -> 2
                    "1" -> 3
                    else -> 999 // Остальные маршруты в конце
                }
            }.thenBy { it.routeNumber }) // Если есть несколько маршрутов с одинаковым приоритетом, сортируем по номеру
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Избранное",
                        style = androidx.compose.ui.text.TextStyle(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default, // Roboto
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        when {
            isLoading -> FLoadingState()
            favoriteTimesList.isEmpty() -> FEmptyState()
            else -> {
                // Отображаем карточки маршрутов в зависимости от режима
                val isGridMode = displayMode == RouteDisplayMode.GRID
                
                if (isGridMode) {
                    // Режим сетки
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(DesignTokens.Spacing.Medium),
                        horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Small + 4.dp),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.Small + 4.dp)
                    ) {
                        items(
                            items = favoriteRoutes,
                            key = { route -> route.id }
                        ) { route ->
                            BusRouteCard(
                                route = route,
                                onClick = { 
                                    try {
                                        navController?.navigate("favorite_route_details/${route.id}") {
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Navigation error to favorite route details: ${route.id}")
                                    }
                                },
                                isGridMode = true,
                                showNotificationButton = false,
                                gridColumns = gridColumns
                            )
                        }
                    }
                } else {
                    // Режим списка
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(vertical = DesignTokens.Spacing.ExtraSmall),
                        verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.ExtraSmall),
                        userScrollEnabled = true
                    ) {
                        items(
                            items = favoriteRoutes,
                            key = { route -> route.id }
                        ) { route ->
                            BusRouteCard(
                                route = route,
                                onClick = { 
                                    try {
                                        navController?.navigate("favorite_route_details/${route.id}") {
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Navigation error to favorite route details: ${route.id}")
                                    }
                                },
                                isGridMode = false,
                                showNotificationButton = true,
                                onNotificationClick = {
                                    try {
                                        navController?.navigate("route_notifications/${route.id}") {
                                            launchSingleTop = true
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Navigation error to route notifications: ${route.id}")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsBus,
                contentDescription = stringResource(R.string.empty_state_icon_description_favorites),
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.no_favorite_times_title_updated),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.no_favorite_times_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
