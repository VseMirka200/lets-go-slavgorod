package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.lets_go_slavgorod.ui.theme.DesignTokens

/**
 * Компоненты стилизованных выпадающих меню
 * 
 * Предоставляет единообразный стиль для всех dropdown меню в приложении.
 * Все меню следуют Material Design 3 принципам с кастомными улучшениями.
 * 
 * @author VseMirka200
 * @version 1.0
 */

/**
 * Стилизованное выпадающее меню с единым дизайном
 * 
 * @param expanded состояние открытия/закрытия меню
 * @param onDismissRequest callback при закрытии меню
 * @param modifier модификатор для настройки
 * @param content содержимое меню
 */
@Composable
fun StyledDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 4.dp),
    content: @Composable () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        shape = RoundedCornerShape(DesignTokens.CornerRadius.Large),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = DesignTokens.Elevation.Level3,
        shadowElevation = DesignTokens.Elevation.Level2
    ) {
        content()
    }
}

/**
 * Стилизованный элемент выпадающего меню
 * 
 * @param text текст элемента
 * @param selected выбран ли элемент
 * @param onClick callback при клике
 * @param modifier модификатор
 * @param enabled активен ли элемент
 * @param leadingIcon иконка слева (опционально)
 */
@Composable
fun StyledDropdownMenuItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null
) {
    DropdownMenuItem(
        text = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    selected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = if (selected || leadingIcon != null) {
            {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Выбрано",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    leadingIcon?.invoke()
                }
            }
        } else null,
        colors = MenuDefaults.itemColors(
            textColor = MaterialTheme.colorScheme.onSurface,
            leadingIconColor = MaterialTheme.colorScheme.primary,
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(
            horizontal = DesignTokens.Spacing.Medium,
            vertical = DesignTokens.Spacing.Small
        )
    )
}

/**
 * Кликабельный элемент для открытия dropdown меню с единым стилем
 * 
 * @param selectedText текущий выбранный элемент
 * @param onClick callback при клике
 * @param modifier модификатор
 * @param label метка (опционально)
 */
@Composable
fun DropdownMenuTrigger(
    selectedText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.Medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = DesignTokens.Elevation.Level1
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = DesignTokens.Spacing.Medium,
                    vertical = DesignTokens.Spacing.Small
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(DesignTokens.Spacing.Small))
            }
            
            Text(
                text = selectedText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = if (label == null) Modifier.weight(1f) else Modifier
            )
            
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.Small))
            
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Открыть меню",
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Обёртка для dropdown меню с триггером
 * 
 * @param selectedText текущий выбранный элемент
 * @param expanded состояние меню
 * @param onExpandedChange callback изменения состояния
 * @param label метка (опционально)
 * @param modifier модификатор
 * @param menuContent содержимое меню
 */
@Composable
fun StyledDropdownMenuWrapper(
    selectedText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    enabled: Boolean = true,
    menuContent: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        DropdownMenuTrigger(
            selectedText = selectedText,
            onClick = { if (enabled) onExpandedChange(true) },
            label = label,
            enabled = enabled
        )
        
        StyledDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            menuContent()
        }
    }
}

