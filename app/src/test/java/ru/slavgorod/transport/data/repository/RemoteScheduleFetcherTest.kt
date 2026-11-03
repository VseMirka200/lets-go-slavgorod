package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RemoteScheduleFetcherTest {

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetchWithRetry uses the latest url from provider`() = runTest {
        val currentUrl =
            java.util.concurrent.atomic.AtomicReference(server.url("/routes-a.json").toString())
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                return when (request.path) {
                    "/routes-a.json" -> MockResponse().setResponseCode(200)
                        .setBody("""{"source":"a"}""")

                    "/routes-b.json" -> MockResponse().setResponseCode(200)
                        .setBody("""{"source":"b"}""")

                    else -> MockResponse().setResponseCode(404)
                }
            }
        }

        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { currentUrl.get() },
            onlineChecker = { true },
            delayProvider = {}
        )

        assertEquals("""{"source":"a"}""", fetcher.fetchWithRetry())

        currentUrl.set(server.url("/routes-b.json").toString())

        assertEquals("""{"source":"b"}""", fetcher.fetchWithRetry())
    }

    @Test
    fun `rejects oversized declared response`() = runTest {
        server.enqueue(MockResponse().setBody("x".repeat(2 * 1024 * 1024 + 1)))
        assertNull(fetcherWithoutRetries().fetchWithRetry())
    }

    @Test
    fun `rejects oversized chunked response without content length`() = runTest {
        server.enqueue(
            MockResponse().setChunkedBody("x".repeat(2 * 1024 * 1024 + 1), 8192)
        )
        assertNull(fetcherWithoutRetries().fetchWithRetry())
    }

    @Test
    fun `accepts response at byte limit`() = runTest {
        val body = "x".repeat(2 * 1024 * 1024)
        server.enqueue(MockResponse().setBody(body))
        assertEquals(body, fetcherWithoutRetries().fetchWithRetry())
    }

    private fun fetcherWithoutRetries() = RemoteScheduleFetcher(
        remoteJsonUrlProvider = { server.url("/schedule.json").toString() },
        onlineChecker = { true },
        retryDelaysMillis = emptyList(),
        delayProvider = {}
    )
}
