package ru.slavgorod.transport.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute

@Composable
internal fun BusRouteCardGrid(
    route: BusRoute,
    onRouteClick: () -> Unit,
    onRouteLongPress: (() -> Unit)?,
    isPinned: Boolean,
    modifier: Modifier = Modifier,
    gridColumns: Int = 2
) {
    val boxBackgroundColor = rememberRouteCardBackgroundColor(routeColor = route.color)
    val interactionSource = remember { MutableInteractionSource() }
    val fontScale = LocalDensity.current.fontScale
    val (cardHeight, cardPadding, spacerHeight) = remember(gridColumns, fontScale) {
        routeGridSizing(gridColumns, fontScale)
    }
    val routeNumberStyle = routeNumberTextStyle(
        routeNumber = route.routeNumber,
        gridColumns = gridColumns
    )
    val routeCardDescription = stringResource(
        R.string.accessibility_route_card_description,
        route.routeNumber,
        route.name
    )

    RouteCardSurface(
        modifier = modifier
            .routeCardClickable(
                contentDescription = routeCardDescription,
                interactionSource = interactionSource,
                onRouteClick = onRouteClick,
                onRouteLongPress = onRouteLongPress
            )
            .heightIn(min = cardHeight),
        backgroundColor = boxBackgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = cardHeight)
                .padding(cardPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (gridColumns >= 4) {
                    CompactGridRouteCardContent(
                        routeNumber = route.routeNumber,
                        routeNumberStyle = routeNumberStyle
                    )
                } else {
                    StandardGridRouteCardContent(
                        routeNumber = route.routeNumber,
                        routeNumberStyle = routeNumberStyle,
                        gridColumns = gridColumns,
                        spacerHeight = spacerHeight
                    )
                }
            }
            if (isPinned) {
                RoutePinIndicator(modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}

@Composable
private fun CompactGridRouteCardContent(
    routeNumber: String,
    routeNumberStyle: TextStyle
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RouteCardBusLabel(MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = routeNumber,
            color = Color.White,
            style = routeNumberStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Visible
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun StandardGridRouteCardContent(
    routeNumber: String,
    routeNumberStyle: TextStyle,
    gridColumns: Int,
    spacerHeight: Dp
) {
    RouteCardBusLabel(
        when (gridColumns) {
            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            else -> MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        }
    )

    Spacer(modifier = Modifier.height(spacerHeight))

    Text(
        text = routeNumber,
        color = Color.White,
        style = routeNumberStyle,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Visible
    )
}

@Composable
private fun RouteCardBusLabel(style: TextStyle) {
    Text(
        text = stringResource(R.string.route_card_bus_label),
        style = style,
        color = Color.White.copy(alpha = 0.9f),
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}
