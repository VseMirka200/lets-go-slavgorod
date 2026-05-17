package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.repository.RoutesTableDataSource
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.schedule.ScheduleList
import ru.slavgorod.transport.ui.model.DeparturePointSchedules
import ru.slavgorod.transport.ui.model.ScheduleUiState
import ru.slavgorod.transport.ui.viewmodel.DisplaySettingsViewModel
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel
import timber.log.Timber
import java.util.Calendar

@Composable
fun ScheduleScreen(
    routeId: String,
    routeRepository: RoutesTableDataSource,
    routesViewModel: RoutesViewModel? = null,
    onBackClick: (() -> Unit)? = null
) {
    LaunchedEffect(routeId) {
        UserActionLogger.screenOpened(R.string.schedule_screen_opened, routeId)
    }

    val actualRoutesViewModel: RoutesViewModel = routesViewModel ?: koinViewModel()
    val displaySettingsViewModel: DisplaySettingsViewModel = koinViewModel()
    val uiState by actualRoutesViewModel.uiState.collectAsStateWithLifecycle()
    val dataUpdateEventId = uiState.dataSourceStatus.updateEventId
    val swapScheduleColumns by displaySettingsViewModel.swapScheduleColumns.collectAsStateWithLifecycle(
        initialValue = false
    )
    val route = remember(routeId, uiState.routes) {
        uiState.routes.find { it.id == routeId }
    }

    val currentTimeTick by produceState(
        initialValue = Calendar.getInstance().timeInMillis,
        key1 = routeId
    ) {
        while (true) {
            value = Calendar.getInstance().timeInMillis
            delay(SCHEDULE_TIME_TICK_INTERVAL_MS)
        }
    }

    val platformPrefixLabel = stringResource(R.string.schedule_platform_prefix_label)
    val weekdayLabel = stringResource(R.string.schedule_day_weekday)
    val weekendLabel = stringResource(R.string.schedule_day_weekend)
    val dailyLabel = stringResource(R.string.schedule_day_daily)

    val loadState by produceState<ScheduleLoadState>(
        initialValue = ScheduleLoadState.Loading,
        key1 = routeId,
        key2 = route,
        key3 = dataUpdateEventId
    ) {
        try {
            if (route == null) {
                return@produceState
            }

            value = ScheduleLoadState.Loaded(
                buildScheduleScreenData(loadRouteSchedules(route, routeRepository))
            )
        } catch (_: CancellationException) {
        } catch (exception: Exception) {
            Timber.tag("Schedule").e(exception, "Unexpected error loading schedule")
            value = ScheduleLoadState.Loaded(ScheduleScreenData(emptyList()))
        }
    }

    when {
        route == null && uiState.isLoading -> LoadingIndicator()
        route == null -> RouteNotFoundMessage(
            routeId = routeId,
            errorMessage = uiState.error,
            modifier = Modifier.fillMaxSize()
        )

        loadState is ScheduleLoadState.Loading -> LoadingIndicator()
        (loadState as? ScheduleLoadState.Loaded)?.data?.totalSchedules == 0 -> EmptyScheduleMessage(
            routeId
        )

        else -> {
            val currentScheduleData = (loadState as ScheduleLoadState.Loaded).data
            val liveScheduleData = remember(currentScheduleData, currentTimeTick) {
                currentScheduleData.withCurrentTime(currentTimeTick)
            }
            val scheduleUiState = ScheduleUiState(
                route = route,
                departurePoints = liveScheduleData.points.map { point ->
                    DeparturePointSchedules(
                        name = point.name,
                        schedules = point.schedules,
                        nextUpcomingId = point.nextUpcomingId
                    )
                }
            )

            ScheduleList(
                scheduleState = scheduleUiState,
                swapScheduleColumns = swapScheduleColumns,
                onBackClick = onBackClick,
                contentPadding = PaddingValues(bottom = 48.dp),
                currentTimeMillis = currentTimeTick,
                scheduleExtraLabelProvider = { schedule ->
                    buildScheduleExtraLabel(
                        schedule = schedule,
                        platformPrefixLabel = platformPrefixLabel,
                        weekdayLabel = weekdayLabel,
                        weekendLabel = weekendLabel,
                        dailyLabel = dailyLabel
                    )
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private sealed interface ScheduleLoadState {
    data object Loading : ScheduleLoadState
    data class Loaded(val data: ScheduleScreenData) : ScheduleLoadState
}

private const val SCHEDULE_TIME_TICK_INTERVAL_MS = 1_000L

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun EmptyScheduleMessage(routeId: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
        ) {
            Text(
                text = stringResource(R.string.schedule_empty_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.schedule_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Timber.w("ScheduleScreen: no schedule entries configured for route %s", routeId)
        }
    }
}

@Composable
private fun RouteNotFoundMessage(
    routeId: String,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Constants.SETTINGS_HORIZONTAL_PADDING.dp)
        ) {
            Text(
                text = stringResource(R.string.schedule_route_not_found_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Constants.SETTINGS_HORIZONTAL_PADDING.dp)
            )
            Text(
                text = errorMessage ?: stringResource(
                    R.string.schedule_route_not_found_body,
                    routeId
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Constants.SETTINGS_HORIZONTAL_PADDING.dp)
            )
        }
    }
}
