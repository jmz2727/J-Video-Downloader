package com.jameel.jvideodownloader

import com.jameel.jvideodownloader.data.MediaTransport
import com.jameel.jvideodownloader.domain.MediaFailure
import com.jameel.jvideodownloader.downloads.TransferEngine
import com.jameel.jvideodownloader.core.MediaSniffer
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.mockwebserver.*
import okio.Buffer
import org.junit.*
import org.junit.Assert.*
import java.io.*
import java.net.InetAddress

class TransportIntegrationTest {
    private lateinit var server: MockWebServer
    private lateinit var transport: MediaTransport
    private lateinit var url: String
    @Before fun setup() {
        server = MockWebServer(); server.start(0)
        // Test-only client maps a public-looking fixture hostname to the local test server.
        val client = OkHttpClient.Builder().dns(object : Dns { override fun lookup(hostname: String) = listOf(InetAddress.getByName("127.0.0.1")) }).followRedirects(false).followSslRedirects(false).build()
        transport = MediaTransport(client)
        // Production accepts only ports 80/443. Bind a test request with port 80 and redirect its
        // socket with a test-only interceptor, preserving production URL validation.
        transport = MediaTransport(client.newBuilder().addInterceptor { chain -> chain.proceed(chain.request().newBuilder().url(chain.request().url.newBuilder().host("127.0.0.1").port(server.port).build()).build()) }.build())
        url = "http://fixture.example/fixture.mp4"
    }
    @After fun stop() { server.shutdown() }
    private fun fixture() = javaClass.getResourceAsStream("/fixture.mp4")!!.readBytes()
    @Test fun directMp4StreamAndPersist() = runBlocking {
        val original = fixture()
        server.enqueue(MockResponse().setHeader("Content-Type", "video/mp4").setBody(Buffer().write(original)))
        val file = File.createTempFile("download", ".mp4")
        try {
            transport.open(url, true).use { response ->
                assertEquals("video/mp4", transport.mime(response))
                response.body!!.byteStream().use { input -> file.outputStream().use { output ->
                    TransferEngine().copy(input, output, original.size.toLong(), 1024 * 1024) { _, _ -> }
                } }
            }
            assertArrayEquals(original, file.readBytes()); assertTrue(MediaSniffer.matches(file.readBytes(), "video/mp4"))
            assertEquals("/fixture.mp4", server.takeRequest().path)
        } finally { file.delete() }
    }
    @Test fun rejectsHtmlWithoutDownloadingIt() = runBlocking {
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>private</html>"))
        transport.open(url, true).use { response -> assertThrows(MediaFailure::class.java) { transport.mime(response) } }
    }
    @Test fun restrictedHttpError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        try { transport.open(url, true); fail("must reject") } catch(e: MediaFailure) { assertEquals("RESTRICTED", e.code); assertFalse(e.transient) }
    }
    @Test fun unsafeRedirect() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://localhost/private"))
        try { transport.open(url, true); fail("must reject") } catch(e: MediaFailure) { assertEquals("INVALID_URL", e.code) }
    }
    @Test fun truncatedStreamFails() = runBlocking {
        try { TransferEngine().copy(ByteArrayInputStream(byteArrayOf(1,2)), ByteArrayOutputStream(), 5, 100) { _, _ -> }; fail("must reject") }
        catch(e: MediaFailure) { assertEquals("INCOMPLETE", e.code); assertTrue(e.transient) }
    }
    @Test fun midStreamNetworkFailureIsNotCompletion() = runBlocking {
        val input = object : InputStream() { var read = 0; override fun read(): Int { if(read++ > 3) throw IOException("connection reset"); return 1 } }
        try { TransferEngine().copy(input, ByteArrayOutputStream(), 50, 100) { _, _ -> }; fail("must fail") } catch (_: IOException) { }
    }
    @Test fun cancellationStopsCopy() = runBlocking {
        val started = CompletableDeferred<Unit>()
        val output = ByteArrayOutputStream()
        val input = object : InputStream() {
            override fun read(): Int = 1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                started.complete(Unit)
                Thread.sleep(10)
                java.util.Arrays.fill(buffer, offset, offset + length, 1.toByte())
                return length
            }
        }
        val task = launch(Dispatchers.IO) { TransferEngine().copy(input, output, -1, Long.MAX_VALUE) { _, _ -> } }
        started.await(); task.cancelAndJoin()
        assertTrue(task.isCancelled)
        assertTrue(output.size() < 1024 * 1024)
    }
    @Test fun platformUrlsAreRefusedWithoutNetwork() = runBlocking {
        for (link in listOf("https://youtu.be/test", "https://facebook.com/watch/test", "https://x.com/a/status/1", "https://instagram.com/reel/test")) {
            try { transport.open(link, false); fail("must refuse platform") }
            catch (e: MediaFailure) { assertEquals("PLATFORM_RESTRICTED", e.code) }
        }
        assertEquals(0,server.requestCount)
    }
    @Test fun sizeLimitIsEnforced() = runBlocking {
        try { TransferEngine().copy(ByteArrayInputStream(ByteArray(32)), ByteArrayOutputStream(), 32, 8) { _, _ -> }; fail("must reject") }
        catch(e: MediaFailure) { assertEquals("STORAGE_FULL", e.code) }
    }
}
