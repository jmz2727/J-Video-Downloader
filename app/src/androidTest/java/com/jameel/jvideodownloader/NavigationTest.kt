package com.jameel.jvideodownloader

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun navigationAndInvalidLink() {
        val activity=ui.activity
        ui.onNodeWithText(activity.getString(R.string.url)).performTextInput("invalid")
        ui.onNodeWithText(activity.getString(R.string.analyze)).performClick()
        ui.waitUntil(5000) { ui.onAllNodesWithText(activity.getString(R.string.err_url)).fetchSemanticsNodes().isNotEmpty() }
        ui.onAllNodesWithText(activity.getString(R.string.downloads))[0].performClick()
        ui.onNodeWithText(activity.getString(R.string.search)).assertExists()
        ui.onAllNodesWithText(activity.getString(R.string.history))[0].performClick()
        ui.onNodeWithText(activity.getString(R.string.clear_history)).assertExists()
        ui.onAllNodesWithText(activity.getString(R.string.settings))[0].performClick()
        ui.onNodeWithText(activity.getString(R.string.language)).assertExists()
        ui.onNodeWithText(activity.getString(R.string.dark)).assertExists()
    }
    @Test fun clipboardPaste() {
        val activity=ui.activity
        ui.runOnUiThread { activity.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(android.content.ClipData.newPlainText("link","https://media.example.com/clip.mp4")) }
        ui.onNodeWithText(activity.getString(R.string.paste)).performClick()
        ui.onNodeWithText("https://media.example.com/clip.mp4").assertExists()
    }
}
