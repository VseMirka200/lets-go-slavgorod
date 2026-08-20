package ru.slavgorod.transport.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@Composable
internal fun BusRouteCardList(
    route: BusRoute,
    onRouteClick: () -> Unit,
    onRouteLongPress: (() -> Unit)?,
    isPinned: Boolean,
    modifier: Modifier = Modifier
) {
    val boxBackgroundColor = rememberRouteCardBackgroundColor(routeColor = route.color)
    val interactionSource = remember { MutableInteractionSource() }
    val fontScale = LocalDensity.current.fontScale
    val routeCardDescription = stringResource(
        R.string.accessibility_route_card_description,
        route.routeNumber,
        route.name
    )

    RouteCardSurface(
        modifier = modifier.routeCardClickable(
            contentDescription = routeCardDescription,
            interactionSource = interactionSource,
            onRouteClick = onRouteClick,
            onRouteLongPress = onRouteLongPress
        ),
        backgroundColor = boxBackgroundColor,
        shape = RoundedCornerShape(Constants.CARD_CORNER_RADIUS.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .heightIn(min = scaleDpForFontScale(80.dp, fontScale))
                    .padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = route.routeNumber,
                    color = Color.White,
                    style = listRouteNumberStyle(route.routeNumber),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.width(scaleDpForFontScale(ROUTE_LIST_NUMBER_WIDTH, fontScale))
                )

                Text(
                    text = route.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        lineHeight = MaterialTheme.typography.titleLarge.lineHeight * 1.1f
                    ),
                    color = Color.White,
                    softWrap = true,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp, end = Constants.PADDING_SMALL.dp)
                )
            }
            if (isPinned) {
                RoutePinIndicator(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                )
            }
        }
    }
}
