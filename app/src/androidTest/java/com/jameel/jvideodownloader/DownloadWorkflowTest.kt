package com.jameel.jvideodownloader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jameel.jvideodownloader.data.DownloadRecord
import com.jameel.jvideodownloader.core.MediaPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class DownloadWorkflowTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    @Test fun ownedHttpsFixtureDownloadsPersistsAndDeletesThroughTheUi() {
        val app=ui.activity.application as JVideoApp
        val link="https://raw.githubusercontent.com/jmz2727/J-Video-Downloader/main/app/src/test/resources/fixture.mp4"
        val previous=app.settings.current
        fun record(): DownloadRecord? = runBlocking(Dispatchers.IO) { app.database.downloads().observe().first().firstOrNull { it.originalUrl==link } }
        try {
            // Exercise the valid Android notification-denied path without an OS permission dialog.
            ui.runOnUiThread { app.settings.update(previous.copy(notifications=false,preferredQuality="ask",wifiOnly=false)) }
            ui.onNodeWithText(ui.activity.getString(R.string.url)).performTextInput(link)
            ui.onNodeWithText(ui.activity.getString(R.string.analyze)).performClick()
            ui.waitUntil(45000) { ui.onAllNodesWithText(ui.activity.getString(R.string.choose_quality)).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText(ui.activity.getString(R.string.choose_quality)).performScrollTo().performClick()
            ui.waitUntil(10000) { ui.onAllNodesWithText("Original",substring=true).fetchSemanticsNodes().isNotEmpty() }
            ui.onNodeWithText("Original",substring=true).performClick()
            ui.onNodeWithText(ui.activity.getString(R.string.download)).performScrollTo().performClick()
            ui.waitUntil(60000) { record()?.let { MediaPolicy.terminal(it.status) }==true }
            val saved=record() ?: throw AssertionError("Download history missing")
            assertEquals("Download failed: ${saved.error}","COMPLETED",saved.status)
            assertTrue(saved.bytes>0); assertNotNull(saved.fileUri)
            val file=File(app.filesDir,"media/${saved.fileName}")
            assertTrue(file.isFile); assertEquals(saved.bytes,file.length())
            val fixture=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("fixture.mp4").use { it.readBytes() }
            assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(fixture),MessageDigest.getInstance("SHA-256").digest(file.readBytes()))
            assertTrue(app.storage.exists(saved.fileUri!!))
            ui.activityRule.scenario.recreate()
            ui.onAllNodesWithText(ui.activity.getString(R.string.downloads)).onFirst().performClick()
            ui.onNodeWithText(ui.activity.getString(R.string.completed)).assertExists()
            ui.onNodeWithText(ui.activity.getString(R.string.delete)).performClick()
            ui.onNodeWithText(ui.activity.getString(R.string.confirm)).performClick()
            ui.waitUntil(10000) { record()==null }
            assertFalse(file.exists())
        } finally {
            runBlocking(Dispatchers.IO) {
                record()?.let { row ->
                    if(!MediaPolicy.terminal(row.status)) app.repository.cancel(row.id)
                    app.repository.delete(row.id)
                }
            }
            app.settings.update(previous)
        }
    }
}
