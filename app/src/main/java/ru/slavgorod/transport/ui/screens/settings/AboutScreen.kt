package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.components.settings.SettingsScreenScaffold
import ru.slavgorod.transport.ui.components.settings.SettingsSurfaceCard

private const val DETAILS_URL = "https://vsemirka200.github.io/lets-go-slavgorod/"
private const val PRIVACY_POLICY_URL = "https://vsemirka200.github.io/lets-go-slavgorod/privacy.html"
private const val VK_GROUP_URL = "https://vk.com/letsgoslavgorod"

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    scrollEnabled: Boolean = true
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(R.string.about_screen_title)
    }

    val content: @Composable (Modifier) -> Unit = { rootModifier ->
        Column(
            modifier = rootModifier
                .then(modifier)
                .then(if (showTopBar) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                .padding(
                    start = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                    end = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                    top = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                    bottom = 24.dp
                )
                .then(if (scrollEnabled) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AboutHeaderBlock(appVersion = Constants.APP_VERSION)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AboutDescriptionBlock()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AboutActionRow(
                        title = stringResource(R.string.about_details),
                        icon = Icons.Outlined.Public,
                        onClick = {
                            context.openExternalUrl(
                                url = DETAILS_URL,
                                failureLogMessage = context.getString(R.string.about_open_details_failed)
                            )
                        }
                    )
                    AboutActionRow(
                        title = stringResource(R.string.about_privacy_policy),
                        icon = Icons.Outlined.Public,
                        onClick = {
                            context.openExternalUrl(
                                url = PRIVACY_POLICY_URL,
                                failureLogMessage = context.getString(R.string.about_open_privacy_policy_failed)
                            )
                        }
                    )
                    AboutActionRow(
                        title = stringResource(R.string.about_vk_group),
                        icon = Icons.Outlined.Public,
                        onClick = {
                            context.openExternalUrl(
                                url = VK_GROUP_URL,
                                failureLogMessage = context.getString(R.string.about_open_vk_group_failed)
                            )
                        }
                    )
                }
            }
        }
    }

    SettingsScreenScaffold(
        title = stringResource(R.string.about_screen_title),
        onBackClick = onBackClick,
        showTopBar = showTopBar,
        content = content
    )
}

@Composable
private fun AboutHeaderBlock(
    appVersion: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Constants.SETTINGS_SCREEN_EDGE_PADDING.dp,
                vertical = 4.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_lets_go_slavgorod),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.size(58.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = stringResource(R.string.about_version_format, appVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
            Text(
                text = stringResource(
                    R.string.about_developer_format,
                    stringResource(R.string.developer_name)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun AboutDescriptionBlock(
    modifier: Modifier = Modifier
) {
    SettingsSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        )
    ) {
        Text(
            text = stringResource(R.string.about_app_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun AboutActionRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            Constants.SETTINGS_SCREEN_EDGE_PADDING.dp
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = title,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
