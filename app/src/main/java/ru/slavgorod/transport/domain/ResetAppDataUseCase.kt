package ru.slavgorod.transport.domain

import android.content.Context
import android.content.Intent
import android.os.Process
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CancellationException
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.local.DisclaimerManager
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.local.dataStore
import ru.slavgorod.transport.data.local.displayDataStore
import ru.slavgorod.transport.data.local.themeDataStore
import ru.slavgorod.transport.logging.UserActionLogger
import timber.log.Timber
import java.io.File
import kotlin.system.exitProcess

/** Очищает известные данные приложения и предоставляет отдельную операцию перезапуска. */
class ResetAppDataUseCase(
    private val appContext: Context
) {

    /**
     * Очищает настройки и сохранённые копии расписания. Ошибки отдельных операций
     * записываются в журнал, после чего очистка продолжается; отмена передаётся наружу.
     */
    suspend fun resetApplicationData() {
        UserActionLogger.action(R.string.reset_data_started)
        clearKnownDataStores()
        clearLegacyPreferenceStorage()
        UserActionLogger.action(R.string.reset_data_finished)
    }

    /** Пытается открыть стартовый экран и завершает текущий процесс приложения. */
    fun restartApplication() {
        try {
            UserActionLogger.action(R.string.reset_data_restart)
            val launchIntent = appContext.packageManager
                .getLaunchIntentForPackage(appContext.packageName)
                ?: return

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            appContext.startActivity(launchIntent)
        } catch (exception: Exception) {
            Timber.e(exception, appContext.getString(R.string.reset_data_restart_failed))
        } finally {
            terminateProcess()
        }
    }

    private suspend fun clearKnownDataStores() {
        runClearOperation(R.string.reset_data_store_main) { appContext.dataStore.edit { it.clear() } }
        runClearOperation(R.string.reset_data_store_theme) { appContext.themeDataStore.edit { it.clear() } }
        runClearOperation(R.string.reset_data_store_display) { appContext.displayDataStore.edit { it.clear() } }
        runClearOperation(R.string.reset_data_store_disclaimer) {
            DisclaimerManager.clearState(
                appContext
            )
        }
        runClearOperation(R.string.reset_data_store_schedule_cache) { ScheduleCacheStore(appContext).clear() }
        runClearOperation(R.string.reset_data_store_schedule_cache) {
            android.util.AtomicFile(File(appContext.filesDir, "manual_schedule.json")).delete()
        }
    }

    private fun clearLegacyPreferenceStorage() {
        deleteChildrenSafely(
            directory = File(appContext.filesDir, DATASTORE_DIRECTORY_NAME),
            logMessageResId = R.string.reset_data_store_files
        )
        deleteChildrenSafely(
            directory = File(appContext.filesDir.parent, SHARED_PREFS_DIRECTORY_NAME),
            logMessageResId = R.string.reset_data_store_shared_prefs
        )
    }

    private fun deleteChildrenSafely(directory: File, logMessageResId: Int) {
        try {
            if (!directory.isDirectory) return

            directory.listFiles().orEmpty().forEach { child ->
                if (!child.delete()) {
                    Timber.w(
                        appContext.getString(R.string.reset_failed_remove_child),
                        child.absolutePath
                    )
                }
            }
        } catch (exception: Exception) {
            Timber.e(exception, appContext.getString(logMessageResId))
        }
    }

    internal suspend fun runClearOperation(nameResId: Int, action: suspend () -> Unit) {
        try {
            action()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(
                exception,
                appContext.getString(R.string.reset_failed_clear, appContext.getString(nameResId))
            )
        }
    }

    private fun terminateProcess() {
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }

    private companion object {
        private const val DATASTORE_DIRECTORY_NAME = "datastore"
        private const val SHARED_PREFS_DIRECTORY_NAME = "shared_prefs"
    }
}
