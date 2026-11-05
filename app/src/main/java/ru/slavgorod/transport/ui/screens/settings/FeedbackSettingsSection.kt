package ru.slavgorod.transport.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ru.slavgorod.transport.R
import ru.slavgorod.transport.core.Constants
import ru.slavgorod.transport.ui.components.settings.SettingsMenuRow
import ru.slavgorod.transport.ui.components.settings.SettingsRadioRow

private const val FEEDBACK_EMAIL = "vladislav-olyushin@mail.ru"
private const val FEEDBACK_VK_URL = "https://vk.ru/letsgoslavgorod"

private enum class FeedbackType(@StringRes val title: Int, @StringRes val body: Int) {
    BUG(R.string.feedback_bug, R.string.feedback_email_bug_body),
    SCHEDULE(R.string.feedback_schedule, R.string.feedback_email_schedule_body),
    SUGGESTION(R.string.feedback_suggestion, R.string.feedback_email_suggestion_body),
    OTHER(R.string.feedback_other, R.string.feedback_email_other_body)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeedbackSettingsSection() {
    val context = LocalContext.current
    var selectedType by rememberSaveable { mutableStateOf(FeedbackType.BUG) }
    var showEmailTopics by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.feedback_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )
        SettingsMenuRow(
            title = stringResource(R.string.feedback_vk),
            subtitle = stringResource(R.string.feedback_vk_subtitle),
            icon = Icons.Default.Forum,
            onClick = {
                if (!context.openExternalUrl(FEEDBACK_VK_URL, "Не удалось открыть ВКонтакте")) {
                    Toast.makeText(context, R.string.feedback_open_vk_failed, Toast.LENGTH_LONG).show()
                }
            }
        )
        SettingsMenuRow(
            title = stringResource(R.string.feedback_email),
            subtitle = stringResource(R.string.feedback_email_subtitle),
            icon = Icons.Default.Email,
            onClick = { showEmailTopics = true }
        )
    }

    if (showEmailTopics) {
        SettingsBottomSheet(onDismiss = { showEmailTopics = false }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                SettingsBackButton(onClick = { showEmailTopics = false })
                SettingsSectionTitle(stringResource(R.string.feedback_type_title))
                Column(Modifier.padding(horizontal = 16.dp)) {
                FeedbackType.entries.forEach { type ->
                    SettingsRadioRow(
                        selected = selectedType == type,
                        title = stringResource(type.title),
                        onClick = { selectedType = type }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showEmailTopics = false }) {
                        Text(stringResource(R.string.feedback_email_cancel))
                    }
                    TextButton(onClick = {
                        showEmailTopics = false
                        context.composeFeedbackEmail(selectedType)
                    }) {
                        Text(stringResource(R.string.feedback_email_continue))
                    }
                }
                }
            }
        }
    }
}

private fun Context.composeFeedbackEmail(type: FeedbackType) {
    val subject = getString(R.string.feedback_email_subject, getString(type.title))
    val body = getString(type.body, Constants.APP_VERSION, Build.VERSION.RELEASE.orEmpty())
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, R.string.feedback_open_email_failed, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(this, R.string.feedback_open_email_failed, Toast.LENGTH_LONG).show()
    }
}
