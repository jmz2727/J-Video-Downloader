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
    @Test fun arabicResourcesAndRtlDirection() {
        val config=Configuration(ui.activity.resources.configuration).apply { setLocales(android.os.LocaleList.forLanguageTags("ar")) }
        val localized=ui.activity.createConfigurationContext(config)
        assertEquals("الرئيسية", localized.getString(R.string.home))
        assertEquals(View.LAYOUT_DIRECTION_RTL, localized.resources.configuration.layoutDirection)
        ui.onNodeWithText(ui.activity.getString(R.string.settings)).performClick()
        ui.onNodeWithText("العربية").assertExists()
    }
}
