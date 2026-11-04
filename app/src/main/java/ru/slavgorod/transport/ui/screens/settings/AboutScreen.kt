package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

private const val DETAILS_URL = "https://vsemirka200.github.io/lets-go-slavgorod/"
private const val PRIVACY_POLICY_URL =
    "https://github.com/VseMirka200/lets-go-slavgorod/blob/main/docs/policies/PRIVACY.md"
private const val VK_GROUP_URL = "https://vk.ru/letsgoslavgorod"
private const val MAX_CHANNEL_URL = "https://max.ru/channel_letsgoslavgorod"

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
    scrollEnabled: Boolean = true
) {
    val context = LocalContext.current
    val fontScale = LocalDensity.current.fontScale

    LaunchedEffect(Unit) {
        UserActionLogger.screenOpened(R.string.about_screen_title)
    }

    val content: @Composable (Modifier) -> Unit = { rootModifier ->
        Column(
            modifier = rootModifier
                .then(modifier)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                        end = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                        top = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                        bottom = scaleDpForFontScale(12.dp, fontScale)
                    )
                    .then(if (scrollEnabled) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(8.dp, fontScale))
            ) {
                AboutHeaderBlock()
                AboutDescriptionBlock()
                Column(verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(4.dp, fontScale))) {
                    AboutActionRow(
                        title = stringResource(R.string.about_details),
                        leadingIcon = { iconModifier ->
                            Icon(
                                imageVector = Icons.Outlined.Home,
                                contentDescription = null,
                                modifier = iconModifier,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            context.openExternalUrl(
                                url = DETAILS_URL,
                                failureLogMessage = context.getString(R.string.about_open_details_failed)
                            )
                        }
                    )
                    AboutActionRow(
                        title = stringResource(R.string.about_privacy_policy),
                        leadingIcon = { iconModifier ->
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = iconModifier,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = {
                            context.openExternalUrl(
                                url = PRIVACY_POLICY_URL,
                                failureLogMessage = context.getString(R.string.about_open_privacy_policy_failed)
                            )
                        }
                    )
                }
                AboutSocialFooter(
                    onVkClick = {
                        context.openExternalUrl(
                            url = VK_GROUP_URL,
                            failureLogMessage = context.getString(R.string.about_open_vk_group_failed)
                        )
                    },
                    onMaxClick = {
                        context.openExternalUrl(
                            url = MAX_CHANNEL_URL,
                            failureLogMessage = context.getString(R.string.about_open_max_channel_failed)
                        )
                    }
                )
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
private fun AboutHeaderBlock(modifier: Modifier = Modifier) {
    val fontScale = LocalDensity.current.fontScale
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale),
                vertical = scaleDpForFontScale(4.dp, fontScale)
            ),
        horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(12.dp, fontScale)),
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_lets_go_slavgorod),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier.size(scaleDpForFontScale(58.dp, fontScale))
        )

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(2.dp, fontScale))
        ) {
            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
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
            Text(
                text = stringResource(R.string.about_version_format, Constants.APP_VERSION),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun AboutSocialFooter(
    onVkClick: () -> Unit,
    onMaxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = scaleDpForFontScale(16.dp, fontScale),
                end = scaleDpForFontScale(16.dp, fontScale),
                bottom = scaleDpForFontScale(12.dp, fontScale)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(scaleDpForFontScale(4.dp, fontScale))
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(12.dp, fontScale)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AboutSocialSquareButton(
                iconRes = R.drawable.ic_welcome_vk,
                contentDescription = stringResource(R.string.about_vk_group),
                onClick = onVkClick
            )
            AboutSocialSquareButton(
                iconRes = R.drawable.ic_welcome_max,
                contentDescription = stringResource(R.string.about_max_channel),
                onClick = onMaxClick
            )
        }
    }
}

@Composable
private fun AboutSocialSquareButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    Surface(
        onClick = onClick,
        modifier = modifier.size(scaleDpForFontScale(56.dp, fontScale)),
        shape = RoundedCornerShape(scaleDpForFontScale(14.dp, fontScale)),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = scaleDpForFontScale(1.dp, fontScale)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.CenterVertically)
                .padding(scaleDpForFontScale(10.dp, fontScale)),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun AboutDescriptionBlock(
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    SettingsSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale)
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
    leadingIcon: @Composable (Modifier) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontScale = LocalDensity.current.fontScale
    SettingsSurfaceCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            scaleDpForFontScale(Constants.SETTINGS_SCREEN_EDGE_PADDING.dp, fontScale)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(scaleDpForFontScale(12.dp, fontScale)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon(Modifier.size(scaleDpForFontScale(22.dp, fontScale)))

                Text(
                    modifier = Modifier.weight(1f),
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = title,
                modifier = Modifier.size(scaleDpForFontScale(18.dp, fontScale)),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
