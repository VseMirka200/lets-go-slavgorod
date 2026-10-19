package ru.slavgorod.transport.app.bootstrap

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import ru.slavgorod.transport.core.AppText
import ru.slavgorod.transport.data.local.AppLogStore
import ru.slavgorod.transport.di.appModule
import ru.slavgorod.transport.notifications.AppForegroundTracker
import ru.slavgorod.transport.notifications.ScheduleUpdateNotificationCoordinator
import timber.log.Timber

/**
 * Настраивает зависимости и журналирование до создания первого экрана.
 *
 * Подсказка разработчику: обработчик необработанных исключений должен передать ошибку
 * исходному обработчику, иначе Android не завершит аварийный сценарий штатно.
 */
class BusApplication : Application() {

    private val logStore by lazy(LazyThreadSafetyMode.NONE) {
        AppLogStore(this)
    }

    override fun onCreate() {
        super.onCreate()

        AppText.init(this)
        setupCrashLogging()
        initializeLogging()
        initializeDependencyGraph()
    }

    private fun setupCrashLogging() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Timber.tag("BusApplication").e(throwable, "Необработанное исключение в потоке %s", thread.name)
            } catch (_: Exception) {
                Timber.tag("BusApplication").e(throwable, "Не удалось записать необработанное исключение в журнал")
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun initializeDependencyGraph() {
        try {
            startKoin {
                androidContext(this@BusApplication)
                modules(appModule)
            }
            GlobalContext.get().get<AppForegroundTracker>().install()
            GlobalContext.get().get<ScheduleUpdateNotificationCoordinator>().start()
            Timber.d("Koin инициализирован")
        } catch (exception: Exception) {
            Timber.e(exception, "Не удалось инициализировать Koin: %s", exception.message)
        }
    }

    private fun initializeLogging() {
        try {
            Timber.plant(FileLoggingTree(logStore))

            if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
                Timber.plant(Timber.DebugTree())
                Timber.d("Выполнен Application.onCreate()")
                return
            }

            Timber.plant(
                object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        if (priority < Log.ERROR) return

                        val logTag = tag ?: "Release"
                        System.err.println("[$logTag] $message")
                        t?.let { System.err.println(Log.getStackTraceString(it)) }
                    }
                }
            )
        } catch (exception: Exception) {
            Timber.tag("BusApplication").e(
                exception,
                "Не удалось инициализировать журналирование: %s",
                exception.message
            )
        }
    }

    private class FileLoggingTree(
        private val logStore: AppLogStore
    ) : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            runCatching {
                logStore.append(priority, tag, message, t)
            }
        }
    }
}
