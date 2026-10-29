package com.jameel.jvideodownloader

import androidx.test.core.app.ApplicationProvider
import com.jameel.jvideodownloader.data.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=android.app.Application::class)
class PrivateStorageTest {
    @Before fun resetProviderRootCacheForFreshRobolectricDataDirectory() {
        // Robolectric gives each method a new dataDir; AndroidX caches roots by authority.
        // Real application dataDir is stable. Reset only the test process's cached strategy.
        val field=androidx.core.content.FileProvider::class.java.getDeclaredField("sCache")
        field.isAccessible=true
        (field.get(null) as MutableMap<*, *>).clear()
    }
    @Test fun publishesPrivatelyAndDeletesByScopedUri() = runBlocking {
        val context: android.content.Context=ApplicationProvider.getApplicationContext()
        val storage=MediaStorage(context); val id=UUID.randomUUID().toString()
        val source=storage.temporary(id); source.writeBytes(byteArrayOf(1,2,3))
        val row=DownloadRecord(id,"https://example.com/own.mp4","Own",fileName="$id.mp4",mime="video/mp4")
        var journaled=false
        val uri=storage.publish(row,source) { journaled=true }
        try {
            assertTrue(journaled); assertEquals(context.packageName+".files",uri.authority)
            assertTrue(File(context.filesDir,"media/$id.mp4").isFile)
            assertTrue(storage.exists(uri.toString()))
            storage.delete(uri.toString()); assertFalse(storage.exists(uri.toString()))
            storage.delete(uri.toString()) // already missing is safe
        } finally { source.delete(); File(context.filesDir,"media/$id.mp4").delete() }
    }
    @Test fun cancelledPublicationDoesNotExposePartialFile() = runBlocking {
        val context: android.content.Context=ApplicationProvider.getApplicationContext()
        val storage=MediaStorage(context); val id=UUID.randomUUID().toString()
        val source=storage.temporary(id); source.writeBytes(byteArrayOf(1,2,3))
        val row=DownloadRecord(id,"https://example.com/own.mp4","Own",fileName="$id.mp4",mime="video/mp4")
        try { storage.publish(row,source) { throw CancellationException() }; fail("must cancel") }
        catch(_: CancellationException) { assertFalse(File(context.filesDir,"media/$id.mp4").exists()); assertFalse(File(context.filesDir,"media/.$id.pending").exists()) }
        finally { source.delete() }
    }
}
