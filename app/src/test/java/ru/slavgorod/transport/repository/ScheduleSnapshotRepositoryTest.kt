package ru.slavgorod.transport.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.slavgorod.transport.data.local.ScheduleCacheStore
import ru.slavgorod.transport.data.repository.ScheduleSnapshotRepository
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScheduleSnapshotRepositoryTest {

    private lateinit var cacheStore: ScheduleCacheStore
    private lateinit var repository: ScheduleSnapshotRepository

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        cacheStore = ScheduleCacheStore(context)
        repository = ScheduleSnapshotRepository(cacheStore)
        runTest {
            cacheStore.clear()
        }
    }

    @Test
    fun `readSnapshot returns null when cache file is missing`() = runTest {
        assertFalse(repository.hasSnapshot())
        assertNull(repository.readSnapshot())
    }

    @Test
    fun `writeSnapshot rejects blank json`() = runTest {
        val result = runCatching {
            repository.writeSnapshot("   ")
        }

        assertTrue(result.isFailure)
        assertFalse(repository.hasSnapshot())
    }

    @Test
    fun `writeSnapshot persists readable snapshot`() = runTest {
        repository.writeSnapshot("""{"routes":[]}""")

        val snapshot = repository.readSnapshot()

        assertTrue(repository.hasSnapshot())
        assertNotNull(snapshot)
        assertTrue(snapshot.savedAtMillis > 0L)
    }
}
