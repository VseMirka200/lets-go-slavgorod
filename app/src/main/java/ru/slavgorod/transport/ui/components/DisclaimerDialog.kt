package ru.slavgorod.transport.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.slavgorod.transport.R
import ru.slavgorod.transport.ui.screens.settings.openExternalUrl

private enum class WelcomeStep { NOTICE, COMMUNITY, THANKS }

@Composable
fun DisclaimerDialog(onDismiss: () -> Unit, onAccept: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(WelcomeStep.NOTICE) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().heightIn(max = maxHeight),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        WelcomeTitle(step)
                        when (step) {
                            WelcomeStep.NOTICE -> NoticeContent(acknowledged) { acknowledged = it }
                            WelcomeStep.COMMUNITY -> CommunityContent()
                            WelcomeStep.THANKS -> ThanksContent()
                        }
                    }
                    Button(
                        onClick = {
                            when (step) {
                                WelcomeStep.NOTICE -> if (acknowledged) step = WelcomeStep.COMMUNITY
                                WelcomeStep.COMMUNITY -> step = WelcomeStep.THANKS
                                WelcomeStep.THANKS -> onAccept()
                            }
                        },
                        enabled = step != WelcomeStep.NOTICE || acknowledged,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(stringResource(if (step == WelcomeStep.THANKS) R.string.welcome_finish else R.string.welcome_continue))
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeTitle(step: WelcomeStep) {
    val title = when (step) {
        WelcomeStep.NOTICE -> R.string.disclaimer_title
        WelcomeStep.COMMUNITY -> R.string.welcome_community_title
        WelcomeStep.THANKS -> R.string.welcome_thanks_title
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(Icons.Outlined.Info, contentDescription = null,
                modifier = Modifier.padding(10.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NoticeContent(acknowledged: Boolean, onAcknowledgedChange: (Boolean) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Text(stringResource(R.string.disclaimer_main_text), fontWeight = FontWeight.SemiBold)
    Text(stringResource(R.string.disclaimer_summary), color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
        Text(stringResource(R.string.disclaimer_details), modifier = Modifier.weight(1f, fill = false))
        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
    }
    if (expanded) {
        Text(stringResource(R.string.disclaimer_description_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(
            value = acknowledged, role = Role.Checkbox, onValueChange = onAcknowledgedChange
        ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = acknowledged, onCheckedChange = null)
        Text(stringResource(R.string.disclaimer_accept), modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun CommunityContent() {
    Text(stringResource(R.string.welcome_community_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WelcomeLink("ВКонтакте", R.drawable.ic_welcome_vk, "https://vk.ru/letsgoslavgorod", Modifier.weight(1f))
        WelcomeLink("МАКС", R.drawable.ic_welcome_max, "https://max.ru/channel_letsgoslavgorod", Modifier.weight(1f))
    }
}

@Composable
private fun ThanksContent() {
    Text(stringResource(R.string.welcome_thanks_text), color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WelcomeLink("RuStore", R.drawable.ic_welcome_rustore,
            "https://www.rustore.ru/catalog/app/ru.slavgorod.transport", Modifier.weight(1f))
        WelcomeLink("Google Play", R.drawable.ic_welcome_play,
            "https://play.google.com/store/apps/details?id=ru.slavgorod.transport", Modifier.weight(1f))
    }
}

@Composable
private fun WelcomeLink(label: String, icon: Int, url: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        onClick = {
            val failure = context.getString(R.string.welcome_link_failed)
            if (!context.openExternalUrl(url, failure)) {
                Toast.makeText(context, failure, Toast.LENGTH_SHORT).show()
            }
        },
        modifier = modifier.defaultMinSize(minHeight = 132.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            Image(painterResource(icon), contentDescription = null, modifier = Modifier.size(48.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        }
    }
}
