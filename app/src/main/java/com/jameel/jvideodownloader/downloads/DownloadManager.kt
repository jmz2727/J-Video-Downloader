package com.jameel.jvideodownloader.downloads

import android.content.Context
import androidx.work.*
import com.jameel.jvideodownloader.data.DownloadRecord
import java.util.concurrent.TimeUnit

/** WorkManager persists independent jobs; cancelling one never cancels later items. */
class DownloadManager(context: Context) {
    private val work = WorkManager.getInstance(context)
    fun enqueue(row: DownloadRecord) {
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(workDataOf("id" to row.id))
            .addTag(row.id)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(if (row.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).setRequiresStorageNotLow(true).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        work.enqueueUniqueWork("download-${row.id}", ExistingWorkPolicy.KEEP, request).result.get(30, TimeUnit.SECONDS)
    }
    fun cancel(id: String) { work.cancelUniqueWork("download-$id").result.get(30, TimeUnit.SECONDS) }
}
