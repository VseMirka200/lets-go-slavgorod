package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.app.AppIconButton
import ru.slavgorod.transport.ui.components.app.NoRippleDropdownMenuItem

@Composable
internal fun HomeHeaderActions(
    route: BusRoute?,
    pinnedRouteIds: Set<String>,
    isRefreshing: Boolean,
    moreMenuDescription: String,
    refreshScheduleText: String,
    refreshingScheduleText: String,
    settingsText: String,
    isMenuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onTogglePinned: (BusRoute) -> Unit
) {
    route?.let { pinnedRoute ->
        val isPinned = pinnedRoute.id in pinnedRouteIds
        AppIconButton(
            onClick = { onTogglePinned(pinnedRoute) },
            contentColor = Color.White.copy(alpha = if (isPinned) 1f else 0.82f)
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = if (isPinned) {
                    stringResource(R.string.home_unpin_route)
                } else {
                    stringResource(R.string.home_pin_route)
                },
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer(rotationZ = -28f)
            )
        }
    }

    Box {
        AppIconButton(
            onClick = onMenuOpen,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = moreMenuDescription,
            )
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = onMenuDismiss
        ) {
            NoRippleDropdownMenuItem(
                text = {
                    Text(if (isRefreshing) refreshingScheduleText else refreshScheduleText)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                },
                onClick = {
                    onMenuDismiss()
                    onRefresh()
                }
            )
            NoRippleDropdownMenuItem(
                text = { Text(settingsText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null
                    )
                },
                onClick = {
                    onMenuDismiss()
                    onSettings()
                }
            )
        }
    }
}
