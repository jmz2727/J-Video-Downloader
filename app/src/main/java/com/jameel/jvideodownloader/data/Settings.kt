package com.jameel.jvideodownloader.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Preferences(val language: String = "en", val theme: String = "dark", val wifiOnly: Boolean = false, val notifications: Boolean = true, val location: String? = null, val allowHttp: Boolean = false, val preferredQuality: String = "ask")
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(Preferences(prefs.getString("language", if(context.resources.configuration.locales[0].language == "ar") "ar" else "en")!!, prefs.getString("theme", "dark")!!, prefs.getBoolean("wifi", false), prefs.getBoolean("notifications", true), null, false, prefs.getString("quality", "ask")!!))
    val flow = mutable.asStateFlow()
    val current get() = mutable.value
    fun update(value: Preferences) {
        prefs.edit().putString("language", value.language).putString("theme", value.theme).putBoolean("wifi", value.wifiOnly).putBoolean("notifications", value.notifications).putString("location", null).putBoolean("http", false).putString("quality", value.preferredQuality).apply()
        mutable.value = value.copy(location = null, allowHttp = false)
    }
}
