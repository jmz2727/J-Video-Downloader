package com.jameel.jvideodownloader.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.jameel.jvideodownloader.domain.MediaFailure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File

/** All new files are internal app-private. A content URI grants read access only after explicit Open/Share. */
class MediaStorage(private val context: Context) {
    private val directory get() = File(context.filesDir,"media").apply { if(!exists() && !mkdirs()) throw MediaFailure("STORAGE_ACCESS") }
    fun temporary(id: String) = File(context.cacheDir,"$id.part")
    suspend fun publish(row: DownloadRecord, file: File, onCreated: suspend (Uri) -> Unit): Uri {
        val target=File(directory,row.fileName)
        if(target.canonicalFile.parentFile!=directory.canonicalFile || target.exists()) throw MediaFailure("STORAGE_ACCESS")
        val pending=File(directory,".${row.id}.pending")
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",target)
        try {
            // Journal intended URI before creating any final destination; interrupted copies stay hidden.
            onCreated(uri)
            file.inputStream().use { input -> pending.outputStream().use { output ->
                val bytes=ByteArray(64*1024)
                while(true) {
                    currentCoroutineContext().ensureActive()
                    val count=input.read(bytes); if(count<0) break
                    output.write(bytes,0,count)
                }
                output.fd.sync()
            } }
            currentCoroutineContext().ensureActive()
            if(!pending.renameTo(target)) throw MediaFailure("STORAGE_ACCESS")
            return uri
        } catch(e: Throwable) { pending.delete(); target.delete(); throw e }
    }
    fun exists(uri: String): Boolean = runCatching { context.contentResolver.openFileDescriptor(Uri.parse(uri),"r")?.use { true } ?: false }.getOrDefault(false)
    fun delete(uri: String) {
        val parsed=Uri.parse(uri)
        if(parsed.authority!="${context.packageName}.files" || parsed.pathSegments.firstOrNull()!="media") throw MediaFailure("STORAGE_ACCESS")
        val name=parsed.lastPathSegment ?: throw MediaFailure("STORAGE_ACCESS")
        val file=File(directory,name)
        if(file.canonicalFile.parentFile!=directory.canonicalFile) throw MediaFailure("STORAGE_ACCESS")
        if(file.exists() && !file.delete()) throw MediaFailure("STORAGE_ACCESS")
    }
    fun cleanPending(id: String) { File(directory,".$id.pending").delete() }
}
