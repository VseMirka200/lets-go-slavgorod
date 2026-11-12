package ru.slavgorod.transport.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.slavgorod.transport.data.local.JsonDataSource
import ru.slavgorod.transport.data.repository.ScheduleFetcher
import ru.slavgorod.transport.notifications.AppForegroundTracker

/**
 * Проверяет создание графа Koin после обновления основной версии библиотеки,
 * не заменяя общий для процесса GlobalContext, используемый другими тестами.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class KoinModuleSmokeTest {
    @Test
    fun coreServicesResolveFromIsolatedGraph() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = koinApplication {
            androidContext(context)
            modules(appModule)
        }
        try {
            assertNotNull(application.koin.get<JsonDataSource>())
            assertNotNull(application.koin.get<ScheduleFetcher>())
            assertNotNull(application.koin.get<AppForegroundTracker>())
        } finally {
            application.close()
        }
    }
}
