package com.jameel.jvideodownloader.downloads

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.StatFs
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.jameel.jvideodownloader.JVideoApp
import com.jameel.jvideodownloader.MainActivity
import com.jameel.jvideodownloader.R
import com.jameel.jvideodownloader.core.MediaPolicy
import com.jameel.jvideodownloader.core.MediaSniffer
import java.io.BufferedInputStream
import com.jameel.jvideodownloader.domain.MediaFailure
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.withPermit
import java.io.File

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    private val app get() = applicationContext as JVideoApp
    private val dao get() = app.database.downloads()
    private val itemId = inputData.getString("id").orEmpty()
    private val notificationId = (itemId.hashCode() and 0x7fffffff).coerceAtLeast(1)
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (itemId.isEmpty()) return@withContext Result.failure()
        val initial = dao.get(itemId) ?: return@withContext Result.success()
        if (MediaPolicy.terminal(initial.status)) return@withContext Result.success()
        var file: File? = null
        var published: String? = null
        try {
            // Foreground service starts before waiting for the single transfer permit.
            setForeground(foreground(0, -1))
            app.queue.permit.withPermit {
                val row = dao.get(itemId) ?: return@withPermit Result.success()
                if (MediaPolicy.terminal(row.status)) return@withPermit Result.success()
                var boundNetwork: Network? = null
                if (row.wifiOnly) {
                    val cm = applicationContext.getSystemService(ConnectivityManager::class.java)
                    val candidate = cm.activeNetwork ?: return@withPermit Result.retry()
                    if (cm.getNetworkCapabilities(candidate)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return@withPermit Result.retry()
                    boundNetwork = candidate
                }
                if (dao.start(itemId) == 0) return@withPermit Result.success()
                row.stagingUri?.let { app.storage.delete(it); dao.staging(itemId, null) }
                app.storage.cleanPending(itemId)
                applicationContext.cacheDir.listFiles()?.filter { it.name == "$itemId.part" || it.name.startsWith("$itemId-") }?.forEach { it.delete() }
                val signed = row.platform != "Direct"
                val choice = if(signed) {
                    app.extractor.analyze(row.originalUrl, boundNetwork).choices.firstOrNull { it.id == row.selectionId } ?: throw MediaFailure("QUALITY_UNAVAILABLE")
                } else com.jameel.jvideodownloader.domain.MediaChoice("original", "Original", row.mime, row.total, row.mime.startsWith("audio/"), listOf(com.jameel.jvideodownloader.domain.MediaPart(row.originalUrl,row.mime)))
                val free = StatFs(applicationContext.cacheDir.path).availableBytes
                // Multipart staging + remux + atomic private publication can need three copies.
                val maxBytes = ((free - 16L * 1024 * 1024) / if(choice.parts.size>1) 3 else 2).coerceAtLeast(0)
                var received = 0L
                val parts = mutableListOf<File>()
                for((index, part) in choice.parts.withIndex()) {
                    val target = File(applicationContext.cacheDir,"$itemId-$index.part"); parts += target
                    app.transport.open(part.url, false, network = boundNetwork, signed = signed, headers = part.headers).use { response ->
                        val declared = response.header("Content-Type")?.substringBefore(';')?.lowercase()?.trim()
                        val mime = if(signed && declared == "application/octet-stream") part.mime else app.transport.mime(response)
                        if(mime != part.mime) throw MediaFailure("MEDIA_CHANGED")
                        if(response.code==206 || response.header("Content-Range")!=null) throw MediaFailure("INCOMPLETE")
                        val body=response.body ?: throw MediaFailure("UNAVAILABLE")
                        val total=body.contentLength()
                        if(maxBytes<=0 || total>maxBytes-received) throw MediaFailure("STORAGE_FULL")
                        val offset=received
                        val added=BufferedInputStream(body.byteStream()).use { input ->
                            input.mark(32)
                            val prefix=ByteArray(16); var count=0
                            while(count<prefix.size) { val n=input.read(prefix,count,prefix.size-count); if(n<0) break; count+=n }
                            input.reset()
                            if(!MediaSniffer.matches(prefix.copyOf(count),part.mime)) throw MediaFailure("UNSUPPORTED_MEDIA")
                            target.outputStream().use { output -> TransferEngine().copy(input,output,total,maxBytes-offset) { bytes,speed ->
                                if(dao.get(itemId)?.status!="RUNNING") throw CancellationException()
                                val knownTotal=if(choice.parts.size==1) total else -1L
                                dao.progress(itemId,offset+bytes,knownTotal,speed)
                                setForeground(foreground(offset+bytes,knownTotal))
                            } }
                        }
                        received+=added
                    }
                }
                file=if(parts.size==1) parts.single() else app.storage.temporary(itemId).also { NativeMediaMuxer().combine(parts[0],parts[1],it,choice.mime) }
                ensureActive()
                published=app.storage.publish(row,file!!) { uri -> if(dao.staging(itemId,uri.toString())==0) throw CancellationException() }.toString()
                if(dao.complete(itemId,published!!,file!!.length())==0) app.storage.delete(published!!)
                else runCatching { completionNotification() }
                Result.success()
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                published?.let { runCatching { app.storage.delete(it) } }
                // OS stop is retryable; explicit user cancellation was persisted already.
                dao.requeue(itemId, "INTERRUPTED")
            }
            throw e
        } catch (e: Exception) {
            val error = DownloadErrorHandler.classify(e)
            if (error.transient && MediaPolicy.mayRetry(runAttemptCount)) {
                dao.requeue(itemId, error.code)
                Result.retry()
            } else {
                dao.finish(itemId, "FAILED", error.code)
                Result.failure()
            }
        } finally {
            withContext(NonCancellable) {
                file?.delete()
                applicationContext.cacheDir.listFiles()?.filter { it.name == "$itemId.part" || it.name.startsWith("$itemId-") }?.forEach { it.delete() }
                app.storage.cleanPending(itemId)
                val row = dao.get(itemId)
                if (row?.stagingUri != null && row.status != "COMPLETED") runCatching { app.storage.delete(row.stagingUri) }
            }
        }
    }
    private fun completionNotification() {
        if (!app.settings.current.notifications) return
        if (Build.VERSION.SDK_INT >= 33 && androidx.core.content.ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(applicationContext, "downloads").setSmallIcon(R.drawable.ic_download)
            .setContentTitle(applicationContext.getString(R.string.completed)).setContentText(applicationContext.getString(R.string.app_name)).setAutoCancel(true).build()
        manager.notify(notificationId xor 0x40000000, notification)
    }
    private fun foreground(bytes: Long, total: Long): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("downloads", applicationContext.getString(R.string.downloads), NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(applicationContext, 0, Intent(applicationContext, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(applicationContext, "downloads")
            .setSmallIcon(R.drawable.ic_download).setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(if (total > 0) R.string.downloading else R.string.waiting))
            .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
            .setProgress(100, if (total > 0) ((bytes.toDouble() / total) * 100).toInt().coerceIn(0, 100) else 0, total <= 0)
            .build()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else ForegroundInfo(notificationId, notification)
    }
}
