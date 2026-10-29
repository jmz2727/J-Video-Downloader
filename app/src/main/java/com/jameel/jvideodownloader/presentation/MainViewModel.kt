package com.jameel.jvideodownloader.presentation

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.jameel.jvideodownloader.JVideoApp
import com.jameel.jvideodownloader.domain.MediaInfo
import com.jameel.jvideodownloader.downloads.DownloadErrorHandler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as JVideoApp
    val preferences = app.settings.flow
    val history = app.repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    var input by mutableStateOf(""); private set
    var media by mutableStateOf<MediaInfo?>(null); private set
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    var sharedVersion by mutableIntStateOf(0); private set
    fun receive(value: String) { input(value); sharedVersion++ }
    private var analysisEpoch = 0
    private var analysisJob: Job? = null
    fun input(value: String) { analysisEpoch++; analysisJob?.cancel(); busy = false; input = value.take(8192); media = null }
    fun dismissMessage() { message = null }
    fun analyze() {
        if (busy) return
        val url = input
        val epoch = ++analysisEpoch
        busy = true
        analysisJob = viewModelScope.launch {
            try {
                val result = app.repository.analyze(url)
                if(epoch != analysisEpoch) return@launch
                media = if(app.settings.current.preferredQuality == "original" && result.platform == "Direct") result.copy(selectedId = "original") else result
            }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { if(epoch == analysisEpoch) message = DownloadErrorHandler.classify(e).code }
            finally { if(epoch == analysisEpoch) busy = false }
        }
    }
    fun action(block: suspend () -> Unit) { viewModelScope.launch {
        try { block() } catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = DownloadErrorHandler.classify(e).code }
    } }
    fun select(id: String?) { media = media?.copy(selectedId = id) }
    fun download() { val selected = media ?: return; action { app.repository.enqueue(selected); message = "ADDED" } }
    fun cancel(id: String) = action { app.repository.cancel(id) }
    fun retry(id: String) = action { app.repository.retry(id) }
    fun delete(id: String) = action { app.repository.delete(id) }
    fun clear() = action { app.repository.clearHistory() }
    fun fileAction(uri: String?, operation: (String) -> Unit) = action {
        if (uri == null || !withContext(Dispatchers.IO) { app.storage.exists(uri) }) message = "FILE_MISSING"
        else withContext(Dispatchers.Main) { operation(uri) }
    }
    fun update(value: com.jameel.jvideodownloader.data.Preferences) = app.settings.update(value)
}
