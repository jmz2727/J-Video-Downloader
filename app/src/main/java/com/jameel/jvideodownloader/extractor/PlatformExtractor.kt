package com.jameel.jvideodownloader.extractor

import android.content.Context
import com.jameel.jvideodownloader.core.*
import com.jameel.jvideodownloader.domain.*
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Extraction only: no native downloader, FFmpeg, shell, cookies import, auto-update or plaintext remote requests. */
class PlatformExtractor(private val context: Context) {
    private val gate = Semaphore(1)
    suspend fun analyze(url: String, network: android.net.Network? = null): MediaInfo = gate.withPermit {
        MediaPolicy.validate(url, false)
        if (MediaPolicy.platform(url) == "Direct") throw MediaFailure("UNSUPPORTED_MEDIA")
        withContext(Dispatchers.IO) {
            try { YoutubeDL.init(context) } catch (_: Exception) { throw MediaFailure("ENGINE_UNAVAILABLE") }
            val base = File(context.noBackupFilesDir, "youtubedl-android")
            val native = File(context.applicationInfo.nativeLibraryDir)
            val pythonHome = File(base, "packages/python/usr")
            val python = File(native, "libpython.so")
            val executable = verifiedEngine()
            val javascript = File(native, "libqjs.so")
            if (!python.isFile || !executable.isFile || !javascript.isFile) throw MediaFailure("ENGINE_UNAVAILABLE")
            val session = File(context.cacheDir, "extract-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                TlsTunnelProxy(network?.socketFactory ?: javax.net.SocketFactory.getDefault()) { host -> network?.getAllByName(host) ?: java.net.InetAddress.getAllByName(host) }.use { proxy ->
                    val args = listOf(python.absolutePath, executable.absolutePath,
                        "--ignore-config", "--no-plugin-dirs", "--no-remote-components", "--no-cache-dir", "--no-playlist",
                        "--skip-download", "--dump-single-json", "--no-warnings", "--no-geo-bypass",
                        "--socket-timeout", "15", "--retries", "1", "--extractor-retries", "1",
                        "--js-runtimes", "quickjs:${javascript.absolutePath}", "--proxy", proxy.url(), "--", url)
                    val environment = mapOf("PYTHONHOME" to pythonHome.absolutePath,
                        "PYTHONDONTWRITEBYTECODE" to "1", "LD_LIBRARY_PATH" to File(pythonHome,"lib").absolutePath,
                        "SSL_CERT_FILE" to File(pythonHome,"etc/tls/cert.pem").absolutePath,
                        "HOME" to session.absolutePath, "TMPDIR" to session.absolutePath,
                        "HTTP_PROXY" to proxy.url(), "HTTPS_PROXY" to proxy.url(), "ALL_PROXY" to proxy.url(),
                        "NO_PROXY" to "", "no_proxy" to "", "PATH" to native.absolutePath + ":/system/bin")
                    val result = try { runInterruptible {
                        BoundedProcess().execute(args, environment, session, 180, 2 * 1024 * 1024)
                    } } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { throw MediaFailure(if (e.message == "METADATA_TOO_LARGE") "METADATA_TOO_LARGE" else "EXTRACTION_FAILED") }
                    if (result.exit != 0) throw MediaFailure("EXTRACTION_FAILED")
                    FormatCatalog.parse(url, JSONObject(result.stdout))
                }
            } finally { session.deleteRecursively() }
        }
    }
    private fun verifiedEngine(): File {
        val version="2026.10.26"
        val expected="e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3"
        val target=File(context.noBackupFilesDir,"yt-dlp-$version.zip")
        fun digest(file: File): String {
            val hash=java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer=ByteArray(65536)
                while(true) { val n=input.read(buffer); if(n<0) break; hash.update(buffer,0,n) }
            }
            return hash.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        }
        if(target.isFile && digest(target)==expected) return target
        val pending=File(context.noBackupFilesDir,"engine.pending")
        try {
            context.assets.open("yt-dlp-$version.zip").use { input -> pending.outputStream().use { output -> input.copyTo(output,65536); output.fd.sync() } }
            if(digest(pending)!=expected) throw MediaFailure("ENGINE_UNAVAILABLE")
            if(target.exists() && !target.delete()) throw MediaFailure("ENGINE_UNAVAILABLE")
            if(!pending.renameTo(target)) throw MediaFailure("ENGINE_UNAVAILABLE")
            return target
        } finally { pending.delete() }
    }

}

/** Signed media URLs remain in memory only. Persist the original public page and selected format ID. */
object FormatCatalog {
    fun parse(page: String, json: JSONObject): MediaInfo {
        if (json.optBoolean("is_live") || json.optString("_type") == "playlist" || json.optBoolean("has_drm")) throw MediaFailure("UNSUPPORTED_MEDIA")
        val array = json.optJSONArray("formats") ?: throw MediaFailure("NO_FORMATS")
        val formats = (0 until minOf(array.length(), 256)).mapNotNull { array.optJSONObject(it) }.filter {
            !it.optBoolean("has_drm") && it.optString("format_id").matches(Regex("[A-Za-z0-9._-]{1,80}")) &&
                it.optString("protocol") == "https" && runCatching { MediaPolicy.validateMedia(it.optString("url")); true }.getOrDefault(false)
        }
        fun video(f: JSONObject) = f.optString("vcodec").let { it.isNotBlank() && it != "none" }
        fun audio(f: JSONObject) = f.optString("acodec").let { it.isNotBlank() && it != "none" }
        fun bytes(f: JSONObject): Long = f.optLong("filesize", -1).takeIf { it > 0 } ?: f.optLong("filesize_approx", -1).takeIf { it > 0 } ?: -1
        fun headers(f: JSONObject): Map<String,String> {
            val source = f.optJSONObject("http_headers") ?: json.optJSONObject("http_headers") ?: return emptyMap()
            return listOf("User-Agent", "Referer", "Origin").mapNotNull { key ->
                source.optString(key).takeIf { it.isNotBlank() && it.length <= 4096 && it.none { c -> c.code < 32 || c.code == 127 } }?.let { key to it }
            }.toMap()
        }
        fun part(f: JSONObject, mime: String) = MediaPart(f.getString("url"), mime, bytes(f), headers(f))
        val choices = mutableListOf<MediaChoice>()
        val bestWebm = formats.filter { !video(it) && audio(it) && it.optString("ext")=="webm" && (it.optString("acodec").startsWith("vorbis") || android.os.Build.VERSION.SDK_INT>=29 && it.optString("acodec").startsWith("opus")) }.maxByOrNull { it.optDouble("abr", 0.0) }
        val bestAac = formats.filter { !video(it) && audio(it) && it.optString("ext") in listOf("m4a", "mp4") && it.optString("acodec").startsWith("mp4a") }.maxByOrNull { it.optDouble("abr", 0.0) }
        for (format in formats) {
            val id = format.getString("format_id")
            val ext = format.optString("ext")
            val height = format.optInt("height", 0)
            if (!video(format) && audio(format)) {
                val mime = when(ext) { "m4a", "mp4" -> "audio/mp4"; "mp3" -> "audio/mpeg"; "webm" -> "audio/webm"; "ogg", "opus" -> "audio/ogg"; else -> continue }
                choices += MediaChoice(id, "${ext.uppercase()}" + format.optInt("abr", 0).takeIf { it > 0 }?.let { " · $it kbps" }.orEmpty(), mime, bytes(format), true, listOf(part(format, mime)))
            } else if (video(format) && height in 1..2160) {
                if (audio(format) && ext in listOf("mp4", "webm")) {
                    val mime = if(ext == "mp4") "video/mp4" else "video/webm"
                    choices += MediaChoice(id, "${height}p · ${ext.uppercase()} · A/V", mime, bytes(format), false, listOf(part(format,mime)))
                } else if (!audio(format) && ext in listOf("mp4","webm")) {
                    val paired = if(ext=="mp4") bestAac else bestWebm
                    if(paired!=null && FormatRules.canMux(format.optString("vcodec"),paired.optString("acodec"),ext,android.os.Build.VERSION.SDK_INT)) {
                        val a=bytes(format); val b=bytes(paired)
                        val mime=if(ext=="mp4") "video/mp4" else "video/webm"
                        val amime=if(ext=="mp4") "audio/mp4" else "audio/webm"
                        choices += MediaChoice("$id+${paired.getString("format_id")}", "${height}p · ${ext.uppercase()} · A/V", mime, if(a>0 && b>0 && a<Long.MAX_VALUE-b) a+b else -1, false, listOf(part(format,mime),part(paired,amime)))
                    }
                }
            }
        }
        if (choices.isEmpty()) throw MediaFailure("NO_FORMATS")
        val ordered = choices.distinctBy { it.id }.sortedWith(compareBy<MediaChoice> { it.audioOnly }.thenByDescending { it.label.substringBefore('p').toIntOrNull() ?: 0 }).take(80)
        val first = ordered.first()
        return MediaInfo(page, MediaPolicy.sanitize(json.optString("title", "media")), first.mime, first.size, MediaPolicy.platform(page), choices = ordered, duration = json.optDouble("duration").takeIf { it.isFinite() && it > 0 })
    }
}
