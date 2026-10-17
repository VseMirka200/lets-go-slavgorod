package ru.slavgorod.transport.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.components.app.AppIconButton
import ru.slavgorod.transport.ui.components.app.NoRippleDropdownMenuItem

@Composable
internal fun HomeHeaderActions(
    isRefreshing: Boolean,
    moreMenuDescription: String,
    refreshScheduleText: String,
    refreshingScheduleText: String,
    settingsText: String,
    isMenuOpen: Boolean,
    onMenuOpen: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSettings: () -> Unit
) {
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
