package ru.slavgorod.transport.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.SearchBar
import ru.slavgorod.transport.ui.components.app.AppScreenScaffold
import ru.slavgorod.transport.ui.navigation.navigateToSettingsTopLevel
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel

private val RouteCardsOuterPadding = 12.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    routesViewModel: RoutesViewModel? = null,
    statusMessage: String? = null
) {
    val viewModel: RoutesViewModel = routesViewModel ?: koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPullRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val hapticFeedback = LocalHapticFeedback.current
    val pinActionRoute =
        uiState.routes.firstOrNull { route -> route.id == viewModel.pinActionRouteId }
    val homeScreenTitle = stringResource(R.string.home_screen_title)
    val closePinModeDescription = stringResource(R.string.home_close_pin_mode)
    val moreMenuDescription = stringResource(R.string.home_menu_more)
    val refreshScheduleText = stringResource(R.string.home_refresh_schedule)
    val refreshingScheduleText = stringResource(R.string.home_refreshing_schedule)
    val settingsText = stringResource(R.string.home_settings)

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(homeScreenTitle)
    }

    LaunchedEffect(searchQuery) {
        delay(Constants.SEARCH_DEBOUNCE_MS)
        if (viewModel.shouldLogSearchQuery(searchQuery)) {
            UserActionLogger.routeSearchChanged(searchQuery)
            viewModel.markSearchQueryLogged(searchQuery)
        }
    }

    BackHandler(enabled = viewModel.pinActionRouteId != null || viewModel.isHeaderMenuOpen) {
        viewModel.handleBackPressedFromHome()
    }

    AppScreenScaffold(
        modifier = Modifier.fillMaxSize(),
        title = stringResource(id = R.string.app_name),
        onBackClick = pinActionRoute?.let { { viewModel.clearPinActionRoute() } },
        navigationIcon = Icons.Filled.Close,
        navigationContentDescription = closePinModeDescription,
        statusMessage = statusMessage,
        actions = {
            HomeHeaderActions(
                route = pinActionRoute,
                pinnedRouteIds = uiState.pinnedRouteIds,
                isRefreshing = isPullRefreshing,
                moreMenuDescription = moreMenuDescription,
                refreshScheduleText = refreshScheduleText,
                refreshingScheduleText = refreshingScheduleText,
                settingsText = settingsText,
                isMenuOpen = viewModel.isHeaderMenuOpen,
                onMenuOpen = {
                    UserActionLogger.menuOpened(homeScreenTitle)
                    viewModel.openHeaderMenu()
                },
                onMenuDismiss = { viewModel.dismissHeaderMenu() },
                onRefresh = {
                    viewModel.clearTransientHomeUiState()
                    viewModel.refresh(homeScreenTitle)
                },
                onSettings = {
                    UserActionLogger.action(settingsText)
                    viewModel.clearTransientHomeUiState()
                    navController.navigateToSettingsTopLevel()
                },
                onTogglePinned = { route ->
                    viewModel.toggleRoutePinned(route.id)
                    viewModel.clearPinActionRoute()
                }
            )
        },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = RouteCardsOuterPadding)
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onSearch = {}
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PullToRefreshBox(
                    isRefreshing = isPullRefreshing,
                    onRefresh = { viewModel.refresh(homeScreenTitle) },
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    when {
                        uiState.isLoading -> LoadingState()
                        uiState.error != null -> ErrorState(
                            errorMessage = uiState.error.orEmpty(),
                            onRetry = { viewModel.refresh(homeScreenTitle) }
                        )

                        uiState.routes.isEmpty() -> EmptyState(searchQuery = searchQuery)
                        else -> RoutesListState(
                            routes = uiState.routes,
                            pinnedRouteIds = uiState.pinnedRouteIds,
                            onRoutePinActionReveal = { routeId ->
                                viewModel.showPinAction(routeId)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}
