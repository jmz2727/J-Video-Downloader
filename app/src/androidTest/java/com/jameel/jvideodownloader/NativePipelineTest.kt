package com.jameel.jvideodownloader

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jameel.jvideodownloader.core.BoundedProcess
import com.jameel.jvideodownloader.data.DownloadRecord
import com.jameel.jvideodownloader.data.MediaStorage
import com.jameel.jvideodownloader.downloads.NativeMediaMuxer
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NativePipelineTest {
    @Test fun packagedPythonEngineAndQuickJsExecuteOnAndroid() {
        val context: Context=ApplicationProvider.getApplicationContext()
        YoutubeDL.init(context)
        val native=File(context.applicationInfo.nativeLibraryDir)
        val home=File(context.noBackupFilesDir,"youtubedl-android/packages/python/usr")
        val session=File(context.cacheDir,"native-test-${UUID.randomUUID()}").apply { mkdirs() }
        val engine=File(session,"engine.zip")
        try {
            context.assets.open("yt-dlp-2026.10.26.zip").use { input -> engine.outputStream().use { input.copyTo(it,65536) } }
            val ca=File(home,"etc/tls/cert.pem"); assertTrue("Bundled TLS roots missing",ca.isFile)
            val env=mapOf("PYTHONHOME" to home.absolutePath,"PYTHONDONTWRITEBYTECODE" to "1","LD_LIBRARY_PATH" to File(home,"lib").absolutePath,"SSL_CERT_FILE" to ca.absolutePath,"HOME" to session.absolutePath,"TMPDIR" to session.absolutePath,"PATH" to native.absolutePath+":/system/bin")
            val python=BoundedProcess().execute(listOf(File(native,"libpython.so").absolutePath,engine.absolutePath,"--ignore-config","--no-plugin-dirs","--no-remote-components","--version"),env,session,45,4096)
            assertEquals(0,python.exit); assertEquals("2026.10.26",python.stdout.trim())
            val quickjs=BoundedProcess().execute(listOf(File(native,"libqjs.so").absolutePath,"-e","console.log(1+1)"),env,session,15,4096)
            assertEquals(0,quickjs.exit); assertEquals("2",quickjs.stdout.trim())
        } finally { session.deleteRecursively() }
    }
    @Test fun remuxesRealFixtureAndPublishesOnlyInsidePrivateStorage() = runBlocking {
        val context: Context=ApplicationProvider.getApplicationContext()
        val assets=InstrumentationRegistry.getInstrumentation().context.assets
        val id=UUID.randomUUID().toString()
        val session=File(context.cacheDir,"mux-test-$id").apply { mkdirs() }
        val video=File(session,"video.mp4"); val audio=File(session,"audio.m4a"); val output=File(session,"merged.mp4")
        val storage=MediaStorage(context)
        var uri: String?=null
        try {
            assets.open("fixture.mp4").use { input -> video.outputStream().use { input.copyTo(it,65536) } }
            assets.open("fixture.m4a").use { input -> audio.outputStream().use { input.copyTo(it,65536) } }
            NativeMediaMuxer().combine(video,audio,output,"video/mp4")
            assertTrue(output.length()>0)
            val probe=MediaExtractor()
            try {
                probe.setDataSource(output.absolutePath)
                val mimes=(0 until probe.trackCount).map { probe.getTrackFormat(it).getString(MediaFormat.KEY_MIME) }
                assertTrue(mimes.contains("video/avc")); assertTrue(mimes.contains("audio/mp4a-latm"))
            } finally { probe.release() }
            val row=DownloadRecord(id,"https://example.com/own-fixture.mp4","Owned fixture",fileName="$id.mp4",mime="video/mp4")
            uri=storage.publish(row,output) {}.toString()
            assertTrue(File(context.filesDir,"media/$id.mp4").isFile)
            assertTrue(storage.exists(uri!!))
            storage.delete(uri!!); assertFalse(storage.exists(uri!!)); uri=null
        } finally { uri?.let { storage.delete(it) }; session.deleteRecursively() }
        Unit
    }
}
