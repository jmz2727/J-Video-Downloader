package com.jameel.jvideodownloader

import android.content.res.Configuration
import android.view.View
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.os.LocaleListCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArabicRtlTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    @Test fun switchingLanguageRendersArabicAndRtl() {
        ui.onNodeWithText(ui.activity.getString(R.string.settings)).performClick()
        ui.onNodeWithText("العربية").performClick()
        try {
            ui.waitUntil(10000) { ui.onAllNodesWithText("الإعدادات").fetchSemanticsNodes().isNotEmpty() }
            assertEquals("ar",ui.activity.resources.configuration.locales[0].language)
            assertEquals(View.LAYOUT_DIRECTION_RTL,ui.activity.resources.configuration.layoutDirection)
            ui.onNodeWithText("الرئيسية").performClick()
            ui.onNodeWithText("رابط الوسائط").assertExists()
        } finally {
            ui.runOnUiThread {
                val app=ui.activity.application as JVideoApp
                app.settings.update(app.settings.current.copy(language="en"))
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
            }
        }
    }
}
