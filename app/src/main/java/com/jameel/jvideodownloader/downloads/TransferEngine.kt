package com.jameel.jvideodownloader.downloads

import com.jameel.jvideodownloader.domain.MediaFailure
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.InputStream
import java.io.OutputStream

/** Streams at constant memory. No entire media object is buffered. */
class TransferEngine {
    suspend fun copy(input: InputStream, output: OutputStream, expected: Long, maxBytes: Long, progress: suspend (Long, Long) -> Unit): Long {
        val buffer = ByteArray(64 * 1024)
        var written = 0L
        var lastBytes = 0L
        var lastTime = System.nanoTime()
        while (true) {
            currentCoroutineContext().ensureActive()
            val count = input.read(buffer)
            if (count < 0) break
            written += count
            if (written > maxBytes) throw MediaFailure("STORAGE_FULL")
            if (expected >= 0 && written > expected) throw MediaFailure("INCOMPLETE")
            output.write(buffer, 0, count)
            val now = System.nanoTime()
            if (now - lastTime >= 1_000_000_000L) {
                progress(written, ((written - lastBytes) * 1_000_000_000L / (now - lastTime)))
                lastBytes = written; lastTime = now
            }
        }
        if (written == 0L || expected >= 0 && written != expected) throw MediaFailure("INCOMPLETE", true)
        output.flush()
        progress(written, 0)
        return written
    }
}
