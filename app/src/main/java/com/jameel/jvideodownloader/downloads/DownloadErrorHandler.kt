package com.jameel.jvideodownloader.downloads

import android.system.ErrnoException
import android.system.OsConstants
import com.jameel.jvideodownloader.domain.MediaFailure
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

object DownloadErrorHandler {
    fun classify(error: Throwable): MediaFailure = when (error) {
        is MediaFailure -> error
        is SecurityException -> MediaFailure("STORAGE_ACCESS")
        is SSLException -> MediaFailure("TLS_ERROR")
        is UnknownHostException -> MediaFailure(if (error.message == "UNSAFE_HOST") "UNSAFE_HOST" else "NETWORK", error.message != "UNSAFE_HOST")
        is IOException -> if (generateSequence<Throwable>(error) { it.cause }.any { it is ErrnoException && it.errno == OsConstants.ENOSPC } || error.message?.contains("ENOSPC") == true) MediaFailure("STORAGE_FULL") else MediaFailure("NETWORK", true)
        else -> MediaFailure("UNEXPECTED")
    }
}
