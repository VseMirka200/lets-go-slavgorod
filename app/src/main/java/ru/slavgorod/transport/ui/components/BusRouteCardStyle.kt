package ru.slavgorod.transport.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.theme.DesignTokens
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@Composable
internal fun rememberRouteCardBackgroundColor(
    routeColor: String?
): Color {
    val primaryColor = MaterialTheme.colorScheme.primary
    val routeCardAlpha = rememberRouteCardAlpha()

    return remember(routeColor, primaryColor, routeCardAlpha) {
        try {
            val baseColor = routeColor?.let { Color(it.toColorInt()) } ?: primaryColor
            baseColor.copy(alpha = routeCardAlpha)
        } catch (_: IllegalArgumentException) {
            primaryColor.copy(alpha = routeCardAlpha)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.routeCardClickable(
    contentDescription: String,
    interactionSource: MutableInteractionSource,
    onRouteClick: () -> Unit,
    onRouteLongPress: (() -> Unit)?
): Modifier {
    return this
        .fillMaxWidth()
        .combinedClickable(
            indication = null,
            interactionSource = interactionSource,
            onClick = onRouteClick,
            onLongClick = onRouteLongPress
        )
        .semantics {
            role = Role.Button
            this.contentDescription = contentDescription
        }
}

internal fun routeGridSizing(
    gridColumns: Int,
    fontScale: Float = 1f
): Triple<Dp, Dp, Dp> {
    return when (gridColumns) {
        1 -> Triple(
            scaleDpForFontScale(DesignTokens.Size.Card.Height.Grid1Column, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Large, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Medium - 4.dp, fontScale)
        )

        2 -> Triple(
            scaleDpForFontScale(DesignTokens.Size.Card.Height.Grid2Columns, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Medium + 4.dp, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Medium, fontScale)
        )

        3 -> Triple(
            scaleDpForFontScale(DesignTokens.Size.Card.Height.Grid3Columns, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Medium, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Small + 4.dp, fontScale)
        )

        else -> Triple(
            scaleDpForFontScale(DesignTokens.Size.Card.Height.Grid4Columns, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Small + 4.dp, fontScale),
            scaleDpForFontScale(DesignTokens.Spacing.Small, fontScale)
        )
    }
}

@Composable
internal fun routeNumberTextStyle(
    routeNumber: String,
    gridColumns: Int
): TextStyle = when {
    gridColumns >= 4 -> {
        if (routeNumber.requiresCompactRouteNumberStyle()) {
            MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight * 0.9f
            )
        } else {
            MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 0.9f
            )
        }
    }

    gridColumns == 3 -> {
        if (routeNumber.requiresCompactRouteNumberStyle()) {
            MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold)
        } else {
            MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold)
        }
    }

    gridColumns == 2 -> {
        when {
            routeNumber.requiresCompactRouteNumberStyle() -> MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold
            )

            else -> MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold)
        }
    }

    else -> MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold)
}

@Composable
internal fun listRouteNumberStyle(routeNumber: String): TextStyle {
    return if (routeNumber.requiresCompactRouteNumberStyle()) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
    } else {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
internal fun RoutePinIndicator(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.PushPin,
        contentDescription = stringResource(R.string.route_card_pinned_description),
        tint = Color.White.copy(alpha = 0.46f),
        modifier = modifier
            .size(16.dp)
            .graphicsLayer(rotationZ = -28f)
    )
}

@Composable
internal fun RouteCardSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        content()
    }
}

@Composable
private fun rememberRouteCardAlpha(): Float {
    return 1f
}

private fun String.requiresCompactRouteNumberStyle(): Boolean {
    return length > 3 || any { it.isLetter() }
}

internal val ROUTE_LIST_NUMBER_WIDTH = 44.dp
