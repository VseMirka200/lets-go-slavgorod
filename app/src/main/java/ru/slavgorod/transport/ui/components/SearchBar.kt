package ru.slavgorod.transport.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.components.app.AppIconButton

private val DefaultSearchBarPadding = PaddingValues(
    top = 16.dp,
    bottom = 8.dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = DefaultSearchBarPadding,
    placeholder: String = "",
    leadingIcon: ImageVector = Icons.Default.Search
) {
    val searchHint = stringResource(R.string.accessibility_search_hint)
    val searchPlaceholder = placeholder.ifBlank {
        stringResource(R.string.search_placeholder)
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding)
            .semantics { contentDescription = searchHint },
        placeholder = { Text(searchPlaceholder) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = searchHint,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                ClearSearchButton(onClear = { onQueryChange("") })
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ClearSearchButton(onClear: () -> Unit) {
    val clearSearchDescription = stringResource(R.string.action_clear_search)

    AppIconButton(
        onClick = onClear,
        modifier = Modifier.semantics {
            contentDescription = clearSearchDescription
        }
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = clearSearchDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
