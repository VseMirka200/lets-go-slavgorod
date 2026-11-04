package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.components.app.AppTopBar
import ru.slavgorod.transport.ui.viewmodel.RoutesViewModel

@Composable
fun RouteStopsScreen(
    routeId: String,
    routesViewModel: RoutesViewModel,
    onBackClick: () -> Unit
) {
    val uiState by routesViewModel.uiState.collectAsStateWithLifecycle()
    val route = uiState.routes.firstOrNull { it.id == routeId }
    val context = LocalContext.current
    val preferredMap by remember(context) { context.preferredMapApp() }
        .collectAsState(initial = PreferredMapApp.SYSTEM)
    var selectedDirection by remember(routeId) { mutableIntStateOf(0) }
    // The repository enriches the route using bundled stops for both cache and network loads.
    val stops = route?.stopNames.orEmpty()
    val visibleStops = if (selectedDirection == 0) stops else stops.asReversed()

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = stringResource(R.string.schedule_title_bus_format, route?.routeNumber.orEmpty()),
            subtitle = route?.name,
            onBackClick = onBackClick
        )
        if (route == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.schedule_route_not_found_title))
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { ScheduleSourceBanner(uiState.dataSourceStatus) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.route_stops_screen_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.route_stops_screen_count, stops.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = route.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (stops.size > 1) {
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            stringResource(R.string.route_stops_outbound),
                            stringResource(R.string.route_stops_inbound)
                        ).forEachIndexed { index, label ->
                            SegmentedButton(
                                selected = selectedDirection == index,
                                onClick = { selectedDirection = index },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { openRouteMap(context, route.name, preferredMap) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 14.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(22.dp))
                    Text(
                        text = stringResource(R.string.route_stops_open_route_map),
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
            if (route.id in setOf("3", "4", "105")) {
                item {
                    Text(
                        text = stringResource(R.string.route_stops_incomplete_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            itemsIndexed(
                items = visibleStops,
                key = { index, stop -> "$selectedDirection-$index-$stop" }
            ) { index, stopName ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        openStopMap(context, stopName, route.name, preferredMap)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stopName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.route_stops_open_stop_map),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.Map,
                            contentDescription = stringResource(R.string.route_stops_open_stop_map),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

