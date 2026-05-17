package ru.slavgorod.transport.ui.screens.settings

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.local.AppLogStore
import timber.log.Timber

internal fun Context.exportApplicationLogs(logStore: AppLogStore): Boolean {
    return runCatching {
        val logFile = logStore.getLogFile()
        if (!logFile.exists()) {
            logStore.ensureLogFile()
        }

        val fileUri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            logStore.getLogFile()
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.settings_log_export_subject))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.settings_log_export_body))
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri(getString(R.string.settings_log_export_subject), fileUri)
        }

        val chooserIntent = Intent.createChooser(
            shareIntent,
            getString(R.string.settings_log_export_chooser_title)
        ).apply {
            if (this@exportApplicationLogs !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        startActivity(chooserIntent)
        true
    }.getOrElse { exception ->
        Timber.e(exception, getString(R.string.settings_log_export_failed))
        false
    }
}
