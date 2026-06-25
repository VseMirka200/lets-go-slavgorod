package ru.slavgorod.transport.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.slavgorod.transport.ui.components.app.AppDestructiveButton
import ru.slavgorod.transport.ui.components.app.AppSecondaryButton
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@Composable
internal fun ResetConfirmationDialog(
    title: String,
    bodyText: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BackHandler {}
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = scaleDpForFontScale(12.dp, fontScale)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = scaleDpForFontScale(420.dp, fontScale))
            ) {
                Column(
                    modifier = Modifier.padding(scaleDpForFontScale(24.dp, fontScale)),
                    verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(16.dp, fontScale))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(12.dp, fontScale))) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = scaleDpForFontScale(12.dp, fontScale),
                                    vertical = scaleDpForFontScale(8.dp, fontScale)
                                ),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(scaleDpForFontScale(18.dp, fontScale))
                                )
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            }
                        }

                        Text(
                            text = bodyText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(12.dp, fontScale))
                    ) {
                        AppDestructiveButton(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(confirmText)
                        }

                        AppSecondaryButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(cancelText)
                        }
                    }
                }
            }
        }
    }
}
