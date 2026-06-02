package ru.slavgorod.transport.ui.components.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.model.BusRoute
import ru.slavgorod.transport.ui.components.schedule.ScheduleResponsiveLayoutSpec

@Composable
fun AppScreenScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationContentDescription: String? = null,
    statusMessage: String? = null,
    actions: @Composable () -> Unit = {},
    topBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            topBar?.invoke() ?: title?.let { resolvedTitle ->
                AppTopBar(
                    title = resolvedTitle,
                    subtitle = subtitle,
                    onBackClick = onBackClick,
                    navigationIcon = navigationIcon,
                    navigationContentDescription = navigationContentDescription,
                    statusMessage = statusMessage,
                    actions = actions
                )
            }
        },
        snackbarHost = snackbarHost,
        bottomBar = bottomBar,
        contentWindowInsets = WindowInsets(0),
        content = content
    )
}

@Composable
fun AppTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    navigationIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    navigationContentDescription: String? = null,
    subtitle: String? = null,
    statusMessage: String? = null,
    actions: @Composable () -> Unit = {}
) {
    AppHeaderCard(
        statusMessage = statusMessage
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                AppIconButton(
                    onClick = onBackClick,
                    contentColor = AppHeaderContentColor
                ) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = navigationContentDescription
                            ?: stringResource(R.string.accessibility_back_button)
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AppTopBarTitle(title = title, subtitle = subtitle)
            }

            actions()
        }
    }
}

@Composable
fun SettingsTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    subtitle: String? = null,
    statusMessage: String? = null,
    actions: @Composable () -> Unit = {}
) {
    AppTopBar(
        title = title,
        onBackClick = onBackClick,
        subtitle = subtitle,
        statusMessage = statusMessage,
        actions = actions
    )
}

@Composable
fun ScheduleTitleBar(
    route: BusRoute?,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AppHeaderCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (onBackClick != null) {
                AppIconButton(
                    onClick = onBackClick,
                    contentColor = AppHeaderContentColor
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.accessibility_back_button)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(
                        R.string.schedule_title_bus_format,
                        route?.routeNumber.orEmpty()
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = AppHeaderContentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
    }
}

@Composable
private fun AppHeaderCard(
    modifier: Modifier = Modifier,
    statusMessage: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppHeaderContainerColor,
            contentColor = AppHeaderContentColor
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
            HeaderStatusBanner(statusMessage = statusMessage)
        }
    }
}

@Composable
internal fun ScheduleHeaderDetails(
    route: BusRoute?,
    layout: ScheduleResponsiveLayoutSpec,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp,
    noteText: String? = null
) {
    if (route == null) return
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppHeaderContainerColor,
            contentColor = AppHeaderContentColor
        ),
        shape = RoundedCornerShape(0.dp)
    ) {
        ScheduleHeaderDetailsContent(
            route = route,
            layout = layout,
            horizontalPadding = horizontalPadding,
            noteText = noteText
        )
    }
}

@Composable
private fun AppTopBarTitle(title: String, subtitle: String?) {
    if (subtitle != null) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = AppHeaderSupportingTextColor
            )
        }
    } else {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun HeaderStatusBanner(statusMessage: String?) {
    AnimatedVisibility(
        visible = !statusMessage.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(durationMillis = 180)) +
                expandVertically(animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
                shrinkVertically(animationSpec = tween(durationMillis = 180))
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Text(
                text = statusMessage.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF101828),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
