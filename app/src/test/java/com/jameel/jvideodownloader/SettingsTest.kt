package com.jameel.jvideodownloader

import androidx.test.core.app.ApplicationProvider
import com.jameel.jvideodownloader.data.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35], application=android.app.Application::class)
class SettingsTest {
    @Test fun settingsSurviveRecreation() = runBlocking {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("settings",0).edit().clear().commit()
        val first=Settings(context)
        val value=first.current.copy(language="ar",theme="light",wifiOnly=true,notifications=false,location="content://documents/tree/test",allowHttp=true,preferredQuality="original")
        first.update(value)
        assertEquals(value.copy(location=null,allowHttp=false), Settings(context).flow.first())
        context.getSharedPreferences("settings",0).edit().clear().commit()
        Unit
    }
}
