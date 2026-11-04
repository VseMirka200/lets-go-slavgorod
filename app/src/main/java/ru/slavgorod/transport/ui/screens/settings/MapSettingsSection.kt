package ru.slavgorod.transport.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.slavgorod.transport.ui.screens.PreferredMapApp
import ru.slavgorod.transport.ui.screens.preferredMapApp
import ru.slavgorod.transport.ui.screens.setPreferredMapApp

@Composable
internal fun MapSettingsSection() {
    val context = LocalContext.current
    val preferred by remember(context) { context.preferredMapApp() }
        .collectAsState(initial = PreferredMapApp.SYSTEM)
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Открывать остановки в", style = MaterialTheme.typography.titleMedium)
        PreferredMapApp.values().forEach { option ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable {
                    scope.launch { context.setPreferredMapApp(option) }
                }.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = option == preferred,
                    onClick = { scope.launch { context.setPreferredMapApp(option) } }
                )
                Text(option.label, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Text(
            "Если выбранное приложение не установлено, откроется его веб-карта. " +
                "Остановки без подтверждённых координат ищутся по названию.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
