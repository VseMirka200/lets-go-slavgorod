package ru.slavgorod.transport.ui.components.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.slavgorod.transport.ui.components.app.AppScreenScaffold
import ru.slavgorod.transport.ui.components.app.SettingsTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenScaffold(
    title: String,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    actions: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit
) {
    if (showTopBar) {
        AppScreenScaffold(
            modifier = modifier,
            topBar = {
                SettingsTopBar(
                    title = title,
                    onBackClick = onBackClick,
                    actions = actions
                )
            },
            snackbarHost = snackbarHost
        ) { paddingValues ->
            content(Modifier.padding(paddingValues))
        }
    } else {
        content(modifier)
    }
}
