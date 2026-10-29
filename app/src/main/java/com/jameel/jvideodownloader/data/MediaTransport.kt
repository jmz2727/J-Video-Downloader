package com.jameel.jvideodownloader.data

import com.jameel.jvideodownloader.core.MediaPolicy
import com.jameel.jvideodownloader.domain.*
import kotlinx.coroutines.*
import okhttp3.*
import java.net.InetAddress
import java.net.URLDecoder
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface PlatformIntegration {
    fun accepts(url: String): Boolean
    suspend fun analyze(url: String, allowHttp: Boolean): MediaInfo
}
class PublicDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = Dns.SYSTEM.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !MediaPolicy.publicAddress(it) }) throw UnknownHostException("UNSAFE_HOST")
        return addresses
    }
}
class MediaTransport(private val client: OkHttpClient = defaultClient()) : PlatformIntegration {
    companion object {
        val types = mapOf("video/mp4" to "mp4", "video/webm" to "webm", "video/quicktime" to "mov", "audio/webm" to "webm", "audio/mpeg" to "mp3", "audio/mp4" to "m4a", "audio/aac" to "aac", "audio/ogg" to "ogg", "audio/flac" to "flac", "audio/wav" to "wav", "audio/x-wav" to "wav")
        fun defaultClient() = OkHttpClient.Builder().dns(PublicDns()).followRedirects(false).followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS).callTimeout(30, TimeUnit.MINUTES)
            .retryOnConnectionFailure(false).build()
    }
    override fun accepts(url: String) = MediaPolicy.platform(url) == "Direct"
    suspend fun open(url: String, allowHttp: Boolean, probe: Boolean = false, network: android.net.Network? = null, signed: Boolean = false, headers: Map<String,String> = emptyMap()): Response {
        // A Wi-Fi job is bound to that Network, so it cannot migrate to cellular.
        val activeClient = if (network == null) client else client.newBuilder()
            .socketFactory(network.socketFactory)
            .connectionPool(ConnectionPool(0, 1, TimeUnit.SECONDS))
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> = network.getAllByName(hostname).toList().also { addresses ->
                    if (addresses.isEmpty() || addresses.any { !MediaPolicy.publicAddress(it) }) throw UnknownHostException("UNSAFE_HOST")
                }
            }).build()
        var next = url
        repeat(6) {
            try { if(signed) MediaPolicy.validateMedia(next) else MediaPolicy.validate(next, allowHttp) } catch (_: Exception) { throw MediaFailure("INVALID_URL") }
            if (!accepts(next)) throw MediaFailure("PLATFORM_RESTRICTED")
            val builder = Request.Builder().url(next).header("User-Agent", "JVideoDownloader/1.0").header("Accept-Encoding", "identity")
            if (headers.isNotEmpty()) {
                for ((name,value) in headers) if(name in listOf("User-Agent","Referer","Origin") && value.length<=4096 && value.none { it.code<32 || it.code==127 }) builder.header(name,value)
            }
            if (probe) builder.header("Range", "bytes=0-31")
            val call = activeClient.newCall(builder.build())
            val response = suspendCancellableCoroutine<Response> { c ->
                c.invokeOnCancellation { call.cancel() }
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: java.io.IOException) { if (c.isActive) c.resumeWithException(e) }
                    override fun onResponse(call: Call, response: Response) {
                        c.resume(response) { _, resource, _ -> resource.close() }
                    }
                })
            }
            if (response.code in listOf(301, 302, 303, 307, 308)) {
                val target = response.header("Location")?.let { response.request.url.resolve(it) }
                val oldScheme = response.request.url.scheme
                response.close()
                if (target == null || oldScheme == "https" && target.scheme != "https") throw MediaFailure("UNSAFE_REDIRECT")
                next = target.toString()
            } else {
                if (!response.isSuccessful) {
                    val code = response.code; response.close()
                    throw MediaFailure(when(code) { 401,403 -> "RESTRICTED"; 404,410 -> "UNAVAILABLE"; 429 -> "RATE_LIMIT"; else -> "HTTP_ERROR" }, MediaPolicy.retryHttp(code))
                }
                return response
            }
        }
        throw MediaFailure("UNSAFE_REDIRECT")
    }
    fun mime(response: Response): String {
        val mime = response.header("Content-Type")?.substringBefore(';')?.lowercase()?.trim()
        if (mime !in types) throw MediaFailure("UNSUPPORTED_MEDIA")
        return mime!!
    }
    override suspend fun analyze(url: String, allowHttp: Boolean) = withContext(Dispatchers.IO) {
        open(url, allowHttp, true).use { response ->
            val declared=response.header("Content-Type")?.substringBefore(';')?.lowercase()?.trim()
            val mime=if(declared==null || declared=="application/octet-stream") {
                val extension=response.request.url.pathSegments.lastOrNull()?.substringAfterLast('.', "")?.lowercase()
                val candidate=types.entries.firstOrNull { it.value==extension }?.key ?: throw MediaFailure("UNSUPPORTED_MEDIA")
                val prefix=ByteArray(16); var count=0
                val stream=response.body?.byteStream() ?: throw MediaFailure("UNAVAILABLE")
                while(count<prefix.size) { val n=stream.read(prefix,count,prefix.size-count); if(n<0) break; count+=n }
                if(!com.jameel.jvideodownloader.core.MediaSniffer.matches(prefix.copyOf(count),candidate)) throw MediaFailure("UNSUPPORTED_MEDIA")
                candidate
            } else mime(response)
            val length = if (response.code == 206) response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1 else response.body?.contentLength() ?: -1
            val title = MediaPolicy.sanitize(runCatching { URLDecoder.decode(response.request.url.pathSegments.lastOrNull().orEmpty().replace("+", "%2B"), "UTF-8") }.getOrDefault("media")).substringBeforeLast('.', "media")
            MediaInfo(url.trim(), title.ifBlank { "media" }, mime, length, choices = listOf(MediaChoice("original","Original",mime,length,mime.startsWith("audio/"),listOf(MediaPart(url.trim(),mime,length)))))
        }
    }
}
