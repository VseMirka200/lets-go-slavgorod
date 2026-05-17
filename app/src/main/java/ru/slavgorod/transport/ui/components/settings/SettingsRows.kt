package ru.slavgorod.transport.ui.components.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SettingsMenuRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector,
    expanded: Boolean = false,
    expandable: Boolean = false
) {
    SettingsRowShell(
        modifier = modifier,
        onClick = onClick,
        leading = { SettingsMenuRowIcon(title = title, icon = icon) },
        content = {
            SettingsRowTextContent(
                title = title,
                subtitle = subtitle
            )
        },
        trailing = {
            Icon(
                imageVector = trailingSettingsRowIcon(
                    expanded = expanded,
                    expandable = expandable
                ),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun SettingsRadioRow(
    selected: Boolean,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    verticalPadding: Dp = 4.dp,
    horizontalPadding: Dp = 10.dp,
    subtitle: String? = null
) {
    SettingsRowShell(
        modifier = modifier,
        onClick = onClick,
        contentHorizontalArrangement = Arrangement.spacedBy(4.dp),
        contentVerticalPadding = verticalPadding,
        contentHorizontalPadding = horizontalPadding,
        leading = {
            RadioButton(
                selected = selected,
                onClick = onClick
            )
        },
        content = {
            SettingsRowTextContent(
                title = title,
                subtitle = subtitle,
                titleStyle = MaterialTheme.typography.bodyMedium,
                subtitleMaxLines = 1
            )
        }
    )
}

@Composable
fun SettingsCheckboxRow(
    checked: Boolean,
    title: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    titleMaxLines: Int = Int.MAX_VALUE,
    contentHorizontalPadding: Dp = 10.dp,
    contentVerticalPadding: Dp = 16.dp
) {
    SettingsRowShell(
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        contentHorizontalArrangement = Arrangement.spacedBy(12.dp),
        contentHorizontalPadding = contentHorizontalPadding,
        contentVerticalPadding = contentVerticalPadding,
        content = {
            SettingsRowTextContent(
                title = title,
                subtitle = subtitle,
                titleStyle = MaterialTheme.typography.bodyLarge,
                titleMaxLines = titleMaxLines
            )
        },
        trailing = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
private fun SettingsMenuRowIcon(
    title: String,
    icon: ImageVector
) {
    Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp)
    )
}

@Composable
internal fun RowScope.SettingsRowTextContent(
    title: String,
    subtitle: String?,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    subtitleMaxLines: Int = Int.MAX_VALUE,
    titleMaxLines: Int = Int.MAX_VALUE
) {
    Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = titleStyle,
            fontWeight = FontWeight.Medium,
            maxLines = titleMaxLines,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )

        subtitle?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = subtitleMaxLines
            )
        }
    }
}

@Composable
internal fun SettingsRowShell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    leading: @Composable (() -> Unit)? = null,
    content: @Composable RowScope.() -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    contentHorizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(12.dp),
    contentHorizontalPadding: Dp = 10.dp,
    contentVerticalPadding: Dp = 16.dp
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = contentHorizontalPadding, vertical = contentVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = contentHorizontalArrangement
    ) {
        leading?.invoke()
        content()
        trailing?.invoke()
    }
}

private fun trailingSettingsRowIcon(
    expanded: Boolean,
    expandable: Boolean
): ImageVector {
    return when {
        expandable && expanded -> Icons.Default.KeyboardArrowUp
        expandable -> Icons.Default.KeyboardArrowDown
        else -> Icons.AutoMirrored.Filled.ArrowForward
    }
}
