package ru.slavgorod.transport.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import java.util.concurrent.atomic.AtomicBoolean

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
    fun `fetch follows web app redirect to JSON response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(302).addHeader("Location", server.url("/published")))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"routes":[{"id":"1"}]}"""))
        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { server.url("/exec").toString() },
            onlineChecker = { true },
            retryDelaysMillis = emptyList()
        )

        assertEquals("""{"routes":[{"id":"1"}]}""", fetcher.fetchWithRetry())
        assertEquals("/exec", server.takeRequest().path)
        assertEquals("/published", server.takeRequest().path)
    }

    @Test
    fun `oversized response is rejected even without content length`() = runTest {
        server.enqueue(MockResponse().setChunkedBody("x".repeat(5 * 1024 * 1024 + 1), 4096))
        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { server.url("/large").toString() },
            onlineChecker = { true },
            retryDelaysMillis = emptyList()
        )
        assertNull(fetcher.fetchWithRetry())
    }

    @Test
    fun `permanent client error is not retried`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { server.url("/missing").toString() },
            onlineChecker = { true },
            delayProvider = {}
        )
        assertNull(fetcher.fetchWithRetry())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `temporary server error is retried`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setBody("{}"))
        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { server.url("/routes").toString() },
            onlineChecker = { true },
            delayProvider = {}
        )
        assertEquals("{}", fetcher.fetchWithRetry())
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `cancellation stops a pending request`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val canceled = AtomicBoolean(false)
        val client = OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun canceled(call: Call) { canceled.set(true) }
        }).build()
        val fetcher = RemoteScheduleFetcher(
            remoteJsonUrlProvider = { server.url("/pending").toString() },
            onlineChecker = { true },
            httpClient = client,
            retryDelaysMillis = emptyList()
        )
        val job = launch { fetcher.fetchWithRetry() }
        runCurrent()
        assertNotNull(server.takeRequest(5, java.util.concurrent.TimeUnit.SECONDS))
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        assertTrue(canceled.get(), "Отмена корутины должна отменять сетевой запрос")
    }

}
