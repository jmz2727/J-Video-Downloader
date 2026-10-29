package com.jameel.jvideodownloader

import android.app.Application
import androidx.room.Room
import com.jameel.jvideodownloader.data.*
import com.jameel.jvideodownloader.downloads.DownloadQueue
import com.jameel.jvideodownloader.downloads.DownloadManager
import com.jameel.jvideodownloader.extractor.PlatformExtractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Small explicit dependency graph; no reflection or DI runtime. */
class JVideoApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            // Repair the small insert/schedule crash window; KEEP preserves existing jobs.
            runCatching {
                database.downloads().observe().first().forEach { row ->
                    if(row.activeKey!=null) manager.enqueue(row)
                    else {
                        cacheDir.listFiles()?.filter { it.name=="${row.id}.part" || it.name.startsWith("${row.id}-") }?.forEach { it.delete() }
                        storage.cleanPending(row.id)
                    }
                }
            }
        }
    }
    val settings by lazy { Settings(this) }
    val database by lazy { Room.databaseBuilder(this, DownloadDatabase::class.java, "downloads.db").addMigrations(MIGRATION_1_2).build() }
    val extractor by lazy { PlatformExtractor(this) }
    val transport by lazy { MediaTransport() }
    val storage by lazy { MediaStorage(this) }
    val queue by lazy { DownloadQueue() }
    val manager by lazy { DownloadManager(this) }
    val repository by lazy { LocalDownloadRepository(database.downloads(), settings, transport, manager, storage, extractor) }
}
