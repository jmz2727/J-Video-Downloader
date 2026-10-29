package com.jameel.jvideodownloader.data

import com.jameel.jvideodownloader.core.MediaPolicy
import com.jameel.jvideodownloader.domain.*
import com.jameel.jvideodownloader.downloads.DownloadManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalDownloadRepository(private val dao: DownloadDao, private val settings: Settings, private val transport: MediaTransport, private val queue: DownloadManager, private val storage: MediaStorage, private val extractor: com.jameel.jvideodownloader.extractor.PlatformExtractor) : DownloadRepository {
    private val mutation = Mutex()
    override val history = dao.observe().map { records -> records.map { row ->
        DownloadItem(row.id, row.originalUrl, row.title, row.platform, row.fileName, row.mime, row.fileUri, row.status, row.total, row.bytes, row.speed, row.created, row.quality, row.error, row.activeKey != null)
    } }
    override suspend fun analyze(url: String): MediaInfo {
        try { MediaPolicy.validate(url, false) } catch (_: Exception) { throw MediaFailure("INVALID_URL") }
        return if (MediaPolicy.platform(url) != "Direct") extractor.analyze(url.trim()) else transport.analyze(url.trim(), false)
    }
    override suspend fun enqueue(media: MediaInfo) = withContext(Dispatchers.IO) {
        mutation.withLock {
            val p = settings.current
            try { MediaPolicy.validate(media.url, false) } catch (_: Exception) { throw MediaFailure("INVALID_URL") }
            val choice = media.selected() ?: throw MediaFailure("SELECT_QUALITY")
            if (dao.observe().first().count { it.activeKey != null } >= 10) throw MediaFailure("QUEUE_FULL")
            val id = UUID.randomUUID().toString()
            val ext = MediaTransport.types[choice.mime] ?: throw MediaFailure("UNSUPPORTED_MEDIA")
            val row = DownloadRecord(id, media.url, media.title, fileName = "${MediaPolicy.sanitize(media.title)}-${id}.$ext", mime = choice.mime, platform = media.platform, quality = choice.label, selectionId = choice.id, total = choice.size, activeKey = MediaPolicy.key(media.url), location = null, wifiOnly = p.wifiOnly, allowHttp = false)
            try { dao.insert(row) } catch (_: android.database.sqlite.SQLiteConstraintException) { throw MediaFailure("DUPLICATE") }
            try { queue.enqueue(row) } catch (e: Exception) { dao.finish(id, "FAILED", "SCHEDULING"); throw MediaFailure("SCHEDULING") }
        }
    }
    override suspend fun retry(id: String) = withContext(Dispatchers.IO) {
        mutation.withLock {
            val row = dao.get(id) ?: return@withLock
            if (!MediaPolicy.terminal(row.status) || row.status == "COMPLETED") return@withLock
            try { if (dao.retry(id, MediaPolicy.key(row.originalUrl)) == 0) return@withLock }
            catch (_: android.database.sqlite.SQLiteConstraintException) { throw MediaFailure("DUPLICATE") }
            try { queue.enqueue(row) } catch (_: Exception) { dao.finish(id, "FAILED", "SCHEDULING"); throw MediaFailure("SCHEDULING") }
        }
    }
    override suspend fun cancel(id: String) = withContext(Dispatchers.IO) {
        mutation.withLock {
            // Persist intent first so a late worker completion cannot resurrect cancelled work.
            dao.finish(id, "CANCELLED")
            queue.cancel(id)
            dao.get(id)?.stagingUri?.let { runCatching { storage.delete(it) } }
            Unit
        }
    }
    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutation.withLock {
            val row = dao.get(id) ?: return@withLock
            if (!MediaPolicy.terminal(row.status)) throw MediaFailure("BUSY")
            row.fileUri?.let { storage.delete(it) }
            dao.remove(id)
        }
    }
    override suspend fun clearHistory() = withContext(Dispatchers.IO) { mutation.withLock { dao.clear() } }
}
