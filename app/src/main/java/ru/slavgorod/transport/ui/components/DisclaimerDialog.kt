package ru.slavgorod.transport.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.components.app.AppButton
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisclaimerDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    val fontScale = LocalDensity.current.fontScale

    LaunchedEffect(Unit) {
        isVisible = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = scaleDpForFontScale(16.dp, fontScale),
                    vertical = scaleDpForFontScale(28.dp, fontScale)
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + scaleIn(initialScale = 0.96f)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .heightIn(max = maxHeight),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 12.dp
                ) {
                    Column(
                        modifier = Modifier.padding(
                            horizontal = scaleDpForFontScale(20.dp, fontScale),
                            vertical = scaleDpForFontScale(20.dp, fontScale)
                        )
                    ) {
                        DisclaimerDialogTitle()
                        Spacer(modifier = Modifier.height(scaleDpForFontScale(14.dp, fontScale)))
                        DisclaimerDialogContent(modifier = Modifier.weight(1f, fill = false))
                        Spacer(modifier = Modifier.height(scaleDpForFontScale(18.dp, fontScale)))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            AppButton(
                                onClick = onAccept
                            ) {
                                Text(
                                    text = stringResource(R.string.disclaimer_accept),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisclaimerDialogTitle() {
    val fontScale = LocalDensity.current.fontScale
    Column(verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(10.dp, fontScale))) {
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
                    contentDescription = stringResource(R.string.disclaimer_warning_icon),
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(scaleDpForFontScale(18.dp, fontScale))
                )
                Text(
                    text = stringResource(R.string.disclaimer_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onError
                )
            }
        }

        Text(
            text = stringResource(R.string.disclaimer_main_text),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 24.sp
        )
    }
}

@Composable
private fun DisclaimerDialogContent(modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = scaleDpForFontScale(2.dp, fontScale)),
        verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(10.dp, fontScale))
    ) {
        DisclaimerParagraph(stringResource(R.string.disclaimer_description_text))
    }
}

@Composable
private fun DisclaimerParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 20.sp
    )
}
