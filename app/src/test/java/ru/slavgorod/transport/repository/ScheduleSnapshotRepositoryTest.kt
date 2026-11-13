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
import kotlin.test.assertEquals
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

    @Test
    fun `new snapshot replaces the old snapshot`() = runTest {
        repository.writeSnapshot("""{"routes":[{"id":"old"}]}""")
        repository.writeSnapshot("""{"routes":[{"id":"new"}]}""")

        assertEquals("""{"routes":[{"id":"new"}]}""", repository.readSnapshot()?.json)
    }

    @Test
    fun `blank replacement leaves previous snapshot intact`() = runTest {
        val original = """{"routes":[{"id":"old"}]}"""
        repository.writeSnapshot(original)
        assertTrue(runCatching { repository.writeSnapshot("  ") }.isFailure)
        assertEquals(original, repository.readSnapshot()?.json)
    }

    @Test
    fun `clear removes snapshot for a newly created repository`() = runTest {
        repository.writeSnapshot("""{"routes":[]}""")
        repository.clear()
        val context: Context = ApplicationProvider.getApplicationContext()
        val reopened = ScheduleSnapshotRepository(ScheduleCacheStore(context))
        assertFalse(reopened.hasSnapshot())
        assertNull(reopened.readSnapshot())
    }
}
