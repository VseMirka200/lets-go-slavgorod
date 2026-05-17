package ru.slavgorod.transport.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp

@Composable
fun SettingsContentLayout(
    title: String,
    showTopBar: Boolean,
    scrollEnabled: Boolean,
    isInlineEmbedded: Boolean,
    horizontalPadding: Dp,
    verticalArrangement: Arrangement.Vertical,
    inlineTitleStyle: TextStyle = MaterialTheme.typography.headlineSmall,
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .contentWidth(showTopBar)
            .padding(horizontal = horizontalPadding)
            .scrollWhenEnabled(scrollEnabled),
        verticalArrangement = verticalArrangement
    ) {
        if (!showTopBar && !isInlineEmbedded && title.isNotBlank()) {
            SettingsInlineTitle(title = title, style = inlineTitleStyle)
        }

        content()
    }
}

@Composable
private fun SettingsInlineTitle(
    title: String,
    style: TextStyle
) {
    Text(
        text = title,
        style = style
    )
}

private fun Modifier.contentWidth(showTopBar: Boolean): Modifier {
    return if (showTopBar) fillMaxSize() else fillMaxWidth()
}

@Composable
private fun Modifier.scrollWhenEnabled(scrollEnabled: Boolean): Modifier {
    return if (scrollEnabled) verticalScroll(rememberScrollState()) else this
}
