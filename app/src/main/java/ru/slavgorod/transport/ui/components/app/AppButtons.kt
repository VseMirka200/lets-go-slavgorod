package ru.slavgorod.transport.ui.components.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import ru.slavgorod.transport.ui.theme.DesignTokens
import ru.slavgorod.transport.ui.theme.scaleDpForFontScale

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = DesignTokens.Size.Button.HorizontalPadding,
        vertical = DesignTokens.Size.Button.VerticalPadding
    ),
    shape: Shape = MaterialTheme.shapes.large,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    content: @Composable () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val layoutDirection = LocalLayoutDirection.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minHeight = scaleDpForFontScale(DesignTokens.Size.Button.Height, fontScale),
            minWidth = scaleDpForFontScale(DesignTokens.Size.Button.MinWidth, fontScale)
        ),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.38f),
            disabledContentColor = contentColor.copy(alpha = 0.38f)
        ),
        contentPadding = contentPadding.scaleForFontScale(fontScale, layoutDirection)
    ) {
        content()
    }
}

@Composable
fun AppSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = DesignTokens.Size.Button.HorizontalPadding,
        vertical = DesignTokens.Size.Button.VerticalPadding
    ),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        content = content
    )
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = DesignTokens.Size.Button.HorizontalPadding,
        vertical = DesignTokens.Size.Button.VerticalPadding
    ),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val layoutDirection = LocalLayoutDirection.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(
            minHeight = scaleDpForFontScale(DesignTokens.Size.Button.Height, fontScale),
            minWidth = scaleDpForFontScale(DesignTokens.Size.Button.MinWidth, fontScale)
        ),
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        contentPadding = contentPadding.scaleForFontScale(fontScale, layoutDirection)
    ) {
        content()
    }
}

@Composable
fun AppDestructiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = DesignTokens.Size.Button.HorizontalPadding,
        vertical = DesignTokens.Size.Button.VerticalPadding
    ),
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit
) {
    AppButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = contentPadding,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        content = content
    )
}

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: androidx.compose.ui.unit.Dp = DesignTokens.Size.Button.IconButtonSize,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val fontScale = LocalDensity.current.fontScale

    Surface(
        modifier = modifier
            .requiredSize(scaleDpForFontScale(size, fontScale))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

private fun PaddingValues.scaleForFontScale(
    fontScale: Float,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection
): PaddingValues {
    return PaddingValues(
        start = calculateStartPadding(layoutDirection).scaled(fontScale),
        top = calculateTopPadding().scaled(fontScale),
        end = calculateEndPadding(layoutDirection).scaled(fontScale),
        bottom = calculateBottomPadding().scaled(fontScale)
    )
}

private fun Dp.scaled(fontScale: Float): Dp = scaleDpForFontScale(this, fontScale)
