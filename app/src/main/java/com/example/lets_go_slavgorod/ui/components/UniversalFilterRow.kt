package com.example.lets_go_slavgorod.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Универсальный компонент фильтров с горизонтальной прокруткой
 * 
 * Особенности:
 * - Горизонтальная прокрутка при переполнении
 * - Поддержка жестов свайпа (автоматически через horizontalScroll)
 * - Универсальная структура для любых типов фильтров
 * - Взаимоисключающие фильтры или независимые (configurable)
 * 
 * @param filters список фильтров для отображения
 * @param selectedFilterId ID выбранного фильтра (null = не выбран)
 * @param onFilterSelected callback при выборе фильтра
 * @param modifier модификатор
 */
@Composable
fun UniversalFilterRow(
    filters: List<FilterItem>,
    selectedFilterId: String?,
    onFilterSelected: (String?) -> Unit,
    useEqualWeights: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (!useEqualWeights) Modifier.horizontalScroll(scrollState) else Modifier
            ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { filter ->
            FilterChip(
                selected = selectedFilterId == filter.id,
                onClick = { 
                    // Если уже выбран - снимаем выбор, иначе выбираем
                    onFilterSelected(if (selectedFilterId == filter.id) null else filter.id)
                },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = if (filter.icon != null) 4.dp else 8.dp)
                    ) {
                        // Иконка (опционально)
                        filter.icon?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        
                        // Текст
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        
                        // Счетчик (опционально) - показывается справа от текста
                        filter.count?.let { count ->
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                modifier = if (useEqualWeights) {
                    Modifier.weight(1f).height(48.dp)
                } else {
                    Modifier.height(48.dp)
                }
            )
        }
    }
}

/**
 * Модель данных для элемента фильтра
 * 
 * @param id уникальный идентификатор фильтра
 * @param label текст на кнопке
 * @param icon иконка (опционально)
 * @param count счетчик элементов (опционально, показывается справа от текста)
 */
data class FilterItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val count: Int? = null
)

