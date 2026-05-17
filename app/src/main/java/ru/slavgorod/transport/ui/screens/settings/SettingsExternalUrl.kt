package ru.slavgorod.transport.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import ru.slavgorod.transport.R
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber

internal fun Context.openExternalUrl(
    url: String,
    failureLogMessage: String
): Boolean =
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        UserActionLogger.action(R.string.external_url_opened, url)
        true
    }.getOrElse { exception ->
        Timber.e(exception, failureLogMessage)
        false
    }
