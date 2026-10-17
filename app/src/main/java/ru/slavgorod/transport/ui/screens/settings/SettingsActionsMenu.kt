package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ru.slavgorod.transport.ui.components.app.AppIconButton
import ru.slavgorod.transport.ui.components.app.NoRippleDropdownMenuItem

@Composable
internal fun SettingsActionsMenu(
    expanded: Boolean,
    menuMoreDescription: String,
    resetMenuItemText: String,
    exportLogsMenuItemText: String,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onResetClick: () -> Unit,
    onExportLogsClick: () -> Unit
) {
    Box {
        AppIconButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = menuMoreDescription
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss
        ) {
            NoRippleDropdownMenuItem(
                text = { Text(resetMenuItemText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null
                    )
                },
                onClick = onResetClick
            )
            NoRippleDropdownMenuItem(
                text = { Text(exportLogsMenuItemText) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null
                    )
                },
                onClick = onExportLogsClick
            )
        }
    }
}
