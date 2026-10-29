package com.jameel.jvideodownloader.domain

import kotlinx.coroutines.flow.Flow

enum class DownloadState { QUEUED, RUNNING, COMPLETED, FAILED, CANCELLED }
data class MediaPart(val url: String, val mime: String, val size: Long = -1, val headers: Map<String,String> = emptyMap())
data class MediaChoice(val id: String, val label: String, val mime: String, val size: Long, val audioOnly: Boolean, val parts: List<MediaPart>)
data class MediaInfo(val url: String, val title: String, val mime: String, val size: Long, val platform: String = "Direct", val quality: String = "Original", val choices: List<MediaChoice> = emptyList(), val selectedId: String? = null, val duration: Double? = null) {
    val audio get() = mime.startsWith("audio/")
    fun selected(): MediaChoice? = choices.firstOrNull { it.id == selectedId }
}
data class DownloadItem(
    val id: String, val originalUrl: String, val title: String, val platform: String,
    val fileName: String, val mime: String, val fileUri: String?, val status: String,
    val total: Long, val bytes: Long, val speed: Long, val created: Long,
    val quality: String, val error: String?, val active: Boolean
)
interface DownloadRepository {
    val history: Flow<List<DownloadItem>>
    suspend fun analyze(url: String): MediaInfo
    suspend fun enqueue(media: MediaInfo)
    suspend fun retry(id: String)
    suspend fun cancel(id: String)
    suspend fun delete(id: String)
    suspend fun clearHistory()
}
class MediaFailure(val code: String, val transient: Boolean = false) : java.io.IOException(code)
